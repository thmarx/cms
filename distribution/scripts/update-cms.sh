#!/usr/bin/env bash

set -Eeuo pipefail

usage() {
    printf '%s\n' \
        'Verwendung:' \
        '  update-cms.sh --check [SERVER] [SSH-BENUTZER] [DOWNLOAD-URL]' \
        '  update-cms.sh --install [SERVER] [SSH-BENUTZER]' \
        '' \
        '--check    Neue Version herunterladen, pruefen und vollstaendig vorbereiten.' \
        '--install  Vorbereitete Version aktivieren und den Dienst anschliessend neu starten.'
}

prompt_if_empty() {
    local variable_name=$1
    local prompt=$2
    local value=${!variable_name:-}

    if [[ -z "$value" ]]; then
        read -r -p "$prompt" value
        printf -v "$variable_name" '%s' "$value"
    fi
}

mode=
case ${1:-} in
    --check)
        mode=check
        shift
        ;;
    --install)
        shift
        ;;
    --help|-h)
        usage
        exit 0
        ;;
    '')
        printf 'Bitte --check oder --install angeben.\n\n' >&2
        usage >&2
        exit 2
        ;;
    --*)
        printf 'Unbekannte Option: %s\n\n' "$1" >&2
        usage >&2
        exit 2
        ;;
    *)
        printf 'Bitte zuerst --check oder --install angeben.\n\n' >&2
        usage >&2
        exit 2
        ;;
esac

server=${1:-}
ssh_user=${2:-}
download_url=${3:-}

prompt_if_empty server "Server (Hostname oder IP): "
prompt_if_empty ssh_user "SSH-Benutzer: "
if [[ "$mode" == check ]]; then
    prompt_if_empty download_url "Download-URL der tar.gz-Datei: "
fi

if [[ ! "$server" =~ ^[a-zA-Z0-9._:-]+$ ]]; then
    printf 'Ungueltiger Servername: %s\n' "$server" >&2
    exit 2
fi

if [[ ! "$ssh_user" =~ ^[a-zA-Z_][a-zA-Z0-9_.-]*$ ]]; then
    printf 'Ungueltiger SSH-Benutzer: %s\n' "$ssh_user" >&2
    exit 2
fi

if [[ "$mode" == check && ! "$download_url" =~ ^https?://[^[:space:]]+$ ]]; then
    printf 'Die Download-URL muss mit http:// oder https:// beginnen.\n' >&2
    exit 2
fi

for command_name in ssh base64 tr cat; do
    command -v "$command_name" >/dev/null 2>&1 || {
        printf 'Auf diesem Rechner fehlt das benoetigte Programm: %s\n' "$command_name" >&2
        exit 1
    }
done

printf '\nVerbindung zu %s@%s wird hergestellt.\n' "$ssh_user" "$server"
printf 'SSH und sudo fragen ihre Passwoerter bei Bedarf selbst ab.\n\n'

# %q verhindert, dass Sonderzeichen in den Argumenten als Shell-Code interpretiert werden.
printf -v quoted_mode '%q' "$mode"
printf -v quoted_url '%q' "$download_url"

remote_script=$(cat <<'REMOTE_SCRIPT'
set -Eeuo pipefail

MODE=$1
DOWNLOAD_URL=${2:-}
SERVICE=cms
INSTALL_DIR=/opt/condation-server
UPDATE_DIR=/opt/condation-server_update
ARCHIVE="$UPDATE_DIR/update.tar.gz"
NEW_DIR="$UPDATE_DIR/condation-server"
READY_FILE="$NEW_DIR/.condation-update-ready"
STAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="/opt/condation-cms-$STAMP"
FAILED_DIR="/opt/condation-server-failed-$STAMP"

old_moved=0
new_activated=0

rollback() {
    local exit_code=${1:-$?}
    trap - ERR INT TERM
    set +e

    printf '\nUpdate fehlgeschlagen (Exit-Code %s). Rollback wird versucht.\n' "$exit_code" >&2

    if (( new_activated == 1 )); then
        sudo systemctl stop "$SERVICE"
        sudo mv -- "$INSTALL_DIR" "$FAILED_DIR"
        sudo mv -- "$BACKUP_DIR" "$INSTALL_DIR"
        sudo systemctl start "$SERVICE"
        printf 'Die fehlgeschlagene neue Version liegt unter %s.\n' "$FAILED_DIR" >&2
    elif (( old_moved == 1 )); then
        sudo mv -- "$BACKUP_DIR" "$INSTALL_DIR"
    fi

    exit "$exit_code"
}

validate_existing_installation() {
    sudo test -d "$INSTALL_DIR" || {
        printf 'Die bestehende Installation %s wurde nicht gefunden.\n' "$INSTALL_DIR" >&2
        return 1
    }
    sudo test -d "$INSTALL_DIR/config" || {
        printf 'Erforderlicher Ordner fehlt: %s/config\n' "$INSTALL_DIR" >&2
        return 1
    }
    sudo test -f "$INSTALL_DIR/server.toml" || {
        printf 'Erforderliche Datei fehlt: %s/server.toml\n' "$INSTALL_DIR" >&2
        return 1
    }
    sudo test -d "$INSTALL_DIR/hosts" || {
        printf 'Erforderlicher Ordner fehlt: %s/hosts\n' "$INSTALL_DIR" >&2
        return 1
    }
}

copy_runtime_data() {
    printf 'Uebernehme aktuelle Konfiguration und Hosts ...\n'
    sudo rm -rf -- "$NEW_DIR/theme" "$NEW_DIR/themes" "$NEW_DIR/hosts" "$NEW_DIR/config"
    sudo rm -f -- "$NEW_DIR/server.toml"
    sudo cp -a -- "$INSTALL_DIR/config" "$NEW_DIR/config"
    sudo cp -a -- "$INSTALL_DIR/server.toml" "$NEW_DIR/server.toml"
    sudo cp -a -- "$INSTALL_DIR/hosts" "$NEW_DIR/hosts"
}

command -v systemctl >/dev/null 2>&1 || {
    printf 'Auf dem Server fehlt das benoetigte Programm: systemctl\n' >&2
    exit 1
}

sudo -v
validate_existing_installation
sudo systemctl cat "$SERVICE" >/dev/null

if [[ "$MODE" == check ]]; then
    for command_name in wget tar; do
        command -v "$command_name" >/dev/null 2>&1 || {
            printf 'Auf dem Server fehlt das benoetigte Programm: %s\n' "$command_name" >&2
            exit 1
        }
    done

    printf 'Lade neue Version herunter ...\n'
    sudo rm -rf -- "$UPDATE_DIR"
    sudo mkdir -- "$UPDATE_DIR"
    sudo wget --output-document="$ARCHIVE" -- "$DOWNLOAD_URL"

    printf 'Pruefe und entpacke Archiv ...\n'
    sudo tar -tzf "$ARCHIVE" >/dev/null
    sudo tar -xzf "$ARCHIVE" -C "$UPDATE_DIR"
    sudo test -d "$NEW_DIR" || {
        printf 'Das Archiv enthaelt nicht den erwarteten Ordner condation-server.\n' >&2
        exit 1
    }

    copy_runtime_data
    sudo touch -- "$READY_FILE"

    printf '\nPruefung erfolgreich. Der laufende Dienst wurde nicht veraendert.\n'
    printf 'Die vorbereitete Version liegt unter %s.\n' "$NEW_DIR"
    printf 'Zum Aktivieren das Skript anschliessend mit --install ausfuehren.\n'
    exit 0
fi

sudo test -d "$NEW_DIR" && sudo test -f "$READY_FILE" || {
    printf 'Keine vollstaendig vorbereitete Version unter %s gefunden.\n' "$NEW_DIR" >&2
    printf 'Bitte zuerst das Skript mit --check ausfuehren.\n' >&2
    exit 1
}

sudo test ! -e "$BACKUP_DIR" || {
    printf 'Das Backup-Ziel existiert bereits: %s\n' "$BACKUP_DIR" >&2
    exit 1
}

# Zwischen Check und Installation koennen sich diese Daten geaendert haben.
copy_runtime_data

trap 'rollback $?' ERR
trap 'rollback 130' INT
trap 'rollback 143' TERM
printf 'Aktiviere vorbereitete Version ...\n'
sudo mv -- "$INSTALL_DIR" "$BACKUP_DIR"
old_moved=1
sudo mv -- "$NEW_DIR" "$INSTALL_DIR"
new_activated=1
sudo rm -f -- "$INSTALL_DIR/.condation-update-ready"

printf 'Starte Dienst %s mit der neuen Version neu ...\n' "$SERVICE"
sudo systemctl restart "$SERVICE"
sudo systemctl is-active --quiet "$SERVICE"
trap - ERR INT TERM

sudo rm -rf -- "$UPDATE_DIR" || printf 'Warnung: %s konnte nicht entfernt werden.\n' "$UPDATE_DIR" >&2

printf '\nUpdate erfolgreich.\n'
printf 'Backup der alten Version: %s\n' "$BACKUP_DIR"
sudo systemctl --no-pager --full status "$SERVICE" || true
REMOTE_SCRIPT
)

# Das Skript wird als Kommando und nicht ueber stdin uebertragen. So bleibt das
# Terminal fuer die interaktiven SSH- und sudo-Passwortabfragen verfuegbar.
remote_payload=$(printf '%s' "$remote_script" | base64 | tr -d '\r\n')
ssh -tt -- "${ssh_user}@${server}" \
    "printf '%s' '${remote_payload}' | base64 --decode | bash -s -- ${quoted_mode} ${quoted_url}"
