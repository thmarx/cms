import { $template } from 'system/template.mjs';
import { $hooks } from 'system/hooks.mjs';

/*
$template.registerTemplateSupplier(
	"myName",
	() => "Thorsten"
)
 */
$hooks.registerAction("template/supplier/add", (context) => {
	context.arguments().get("suppliers").add(
			"myName",
			() => "My name is Thorsten"
	)
	return null;
})

/*
$template.registerTemplateFunction(
	"getHello",
	(name) => "Hello " + name + "!"
)
 */
$hooks.registerAction("template/function/add", (context) => {
	context.arguments().get("functions").add(
			"getHello",
		(name) => "Hello " + name + "!!"
	)
	return null;
})
