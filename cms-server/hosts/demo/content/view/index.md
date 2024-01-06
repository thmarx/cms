## YAML Template.
---
template: views/test.html
type: view
meta:
  title: Content-Template view
content:
  query:
    from: "/"
    excerpt: 250
    order_by: title
    order_direction: asc
    conditions: 
      - name: where
        operator: =
        key: template
        value: content.html