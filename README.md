# cms

cms is a simple java based flat file content management system.
see wiki for more information: [wiki](https://github.com/thmarx/cms/wiki)


# changelog

## 5.1.0

* **FEATURE** Persitent index for metadata [#217](https://github.com/thmarx/cms/issues/217)  
	**Attention**: Refactoring of the page objects requires a migration of your templates
* **FEATURE** Correction for the shortcode syntax [#216](https://github.com/thmarx/cms/issues/216)  
	The comma to separate the parameters is not needed anymore.
* **FEATURE** Markdown support for multiline list items [#215](https://github.com/thmarx/cms/issues/215)
* **FEATURE** New taxonomies added at runtime are reloaded when using the cli comman *host reload* [#213](https://github.com/thmarx/cms/issues/213)
* **FEATURE** introduce more hooks [#218](https://github.com/thmarx/cms/issues/218)


### Migration

#### Query

In template code the property total of the page object has been renamed to totalPages.

#### ShortCodes

Shortcodes have to been changed from [code param1="",param2="" /] to [code param1="" param2="" /]

## 5.0.0

* **BREAKING CHANGE** Introduce module registry [#205](https://github.com/thmarx/cms/issues/205)
* **BREAKING CHANGE** Reload host cli command [#209](https://github.com/thmarx/cms/issues/209)
* **FEATURE** Extension management [#9](https://github.com/thmarx/cms/issues/9)
* **FEATURE** Theme management [#208](https://github.com/thmarx/cms/issues/208)
* **FEATURE** Module managment [#206](https://github.com/thmarx/cms/issues/206)

### Highlights

The new cli commands to manage extension, module and themes


### Migration

The base distribution contains only the thymeleaf template engine module. If any other module is use it has to be installed via the command line:
e.q. `java -jar cms-server-5.0.0.jar module get pebble-module`

The reloading of a host requires to update the template engine. For this, every template engine must implemente a `void updateTheme (Theme theme)` method.

## 4.18.1

Missing hook methods for scripting added

## 4.18.0

* **FEATURE** Make RequestContext available in scripting [#201](https://github.com/thmarx/cms/issues/201)
* **MAINTENANCE** pug4j template engine not longer actively supported [#198](https://github.com/thmarx/cms/issues/198)
* **MAINTENANCE** dependencies updated [#202](https://github.com/thmarx/cms/issues/202)
* **FEATURE** Hooks to change navigations [#200](https://github.com/thmarx/cms/issues/200)
* **FEATURE** relative links in markdown [#197](https://github.com/thmarx/cms/issues/197)
* **MAINTENANCE** deprecate third party markdown implementations [#203](https://github.com/thmarx/cms/issues/203)

## 4.17.0

* **MAINTENANCE** Refactor request processing [#136](https://github.com/thmarx/cms/issues/136)
* **BUGFIX** Problem with shortcodes in markdown rendering bug [#193](https://github.com/thmarx/cms/issues/193)
* **FEATURE** CLI Stop command [#194](https://github.com/thmarx/cms/issues/194)
* **FEATURE** Application and Site Events [#195](https://github.com/thmarx/cms/issues/195)

## 4.16.0

* **FEATURE** Application performance management [#191](https://github.com/thmarx/cms/issues/191)
* **FEATURE** Access variables in markdown [#190](https://github.com/thmarx/cms/issues/190)
* **FEATURE** Simple authentication on folders [#189](https://github.com/thmarx/cms/issues/189)
* **FEATURE** Basic CLI [#8](https://github.com/thmarx/cms/issues/8)
* **MAINTENANCE** Update jetty dependency [#188](https://github.com/thmarx/cms/issues/188)

## 4.15.1

* **BUGFIX** TaxonomyResolver does not set total of items correctly [#187](https://github.com/thmarx/cms/issues/187)

## 4.15.0

* **FEATURE** Cache ResourceBundles in messages [#185](https://github.com/thmarx/cms/issues/185)

## 4.14.1

* **BUGFIX** TaxonomyResolver does not set page properties correctly [#184](https://github.com/thmarx/cms/issues/184)

## 4.14.0

* **BUGFIX** Markdown list block does not render content correct if content contains unescaped dot [#183](https://github.com/thmarx/cms/issues/183)
* **MAINTENANCE** remove optional markdown renderer from distribution [#179](https://github.com/thmarx/cms/issues/179 )
* **MAINTENANCE** update dependencies [#180](https://github.com/thmarx/cms/issues/180)

## 4.13.0

* **FEATURE** support for markdown task lists [#178](https://github.com/thmarx/cms/issues/178)
* **FEATURE** support for markdown table [#176](https://github.com/thmarx/cms/issues/176)
* **FEATURE** support for markdown definition lists [#177](https://github.com/thmarx/cms/issues/177)

## 4.12.0

* **FEATURE** New default markdown renderer [#175](https://github.com/thmarx/cms/issues/175)
* **MAINTENANCE** Do not store unused module config [#173](https://github.com/thmarx/cms/issues/173)

## 4.11.0

* **MAINTENANCE** Code refactorings [#170](https://github.com/thmarx/cms/issues/170)
* **MAINTENANCE** New minimal demo project to getting started faster [#171](https://github.com/thmarx/cms/issues/171)

## 4.10.0

* **FEATURE** Make taxonomies better accessible in template code [#169](https://github.com/thmarx/cms/issues/169)

## 4.9.0

* **MAINTENANCE** update jetty dependency to 12.0.6 [#167](https://github.com/thmarx/cms/issues/167)
* **MAINTENANCE** management events to clear caches and reindex meta data [#158](https://github.com/thmarx/cms/issues/158)
* **BUGFIX** refactor theme loading [#168](https://github.com/thmarx/cms/issues/168)

## 4.8.0

* **MAINTENANCE** optimize request context creation [#165](https://github.com/thmarx/cms/issues/165)
* **MAINTENANCE** refactor theme loading [#166](https://github.com/thmarx/cms/issues/166)

## 4.7.0

* **FEATURE** extend content queries with custom operator [#160](https://github.com/thmarx/cms/issues/160)

## 4.6.0

* **MAINTENANCE** update markedj module [#164](https://github.com/thmarx/cms/issues/164)
* **BUGFIX** fix broken taxonomies [#163](https://github.com/thmarx/cms/issues/163)
* **MAINTENANCE** renaming of taxonomyfn [#162](https://github.com/thmarx/cms/issues/162)

## 4.5.1

* **BUGFIX** vhost not set correctly for module handler [#159](https://github.com/thmarx/cms/issues/159)

## 4.5.0

* **FEATURE** User separate Feature class [#157](https://github.com/thmarx/cms/issues/157)
* **FEATURE** Make environment accessible [#156](https://github.com/thmarx/cms/issues/156)

## 4.4.0

* **FEATURE** example to use hook system in module [#143](https://github.com/thmarx/cms/issues/143)
* **FEATURE** make ModuleManager accessible in module [#155](https://github.com/thmarx/cms/issues/155)

## 4.3.0

* **MAINTENANCE** update modules [#152](https://github.com/thmarx/cms/issues/152)
* **FEATURE** add access to request context to modules [#148](https://github.com/thmarx/cms/issues/148)
* **FEATURE** ExtensionPoint to register shortcodes via module [#153](https://github.com/thmarx/cms/issues/153)

## 4.2.0

* **MAINTENANCE** update seo module [#150](https://github.com/thmarx/cms/issues/150)
* **FEATURE** override site config per environment [#149](https://github.com/thmarx/cms/issues/149)

## 4.1.0

* **FEATURE** make current node and taxonomy accessible [#147](https://github.com/thmarx/cms/issues/147)

## 4.0.0

* **MAINTENANCE** remove legacy theme assets [#145](https://github.com/thmarx/cms/issues/145)
* **MAINTENANCE** refactor module context to use features [#135](https://github.com/thmarx/cms/issues/135)
**ATTENTION:** Migration required!!!
* **MAINTENANCE** move modules to separate repositories [#110](https://github.com/thmarx/cms/issues/110)
* **FEATURE** custom http routes [#142](https://github.com/thmarx/cms/issues/142)
* **FEATURE** views introduced [#80](https://github.com/thmarx/cms/issues/80)

## 3.3.0

* **FEATURE** HookSystem [#99](https://github.com/thmarx/cms/issues/99)
* **MAINTENANCE** rename content tags to shortcodes [#134](https://github.com/thmarx/cms/issues/134)
* **FEATURE** Improvement for forms module [#133](https://github.com/thmarx/cms/issues/133)

## 3.2.1
* **BUGFIX** avoid exception on missing taxonomy config file

## 3.2.0
* **FEATURE** forms-module [#17](https://github.com/thmarx/cms/issues/17)
* **FEATURE** Add HTTPUtils to api [#128](https://github.com/thmarx/cms/issues/128)
* **MAINTENANCE** Update Jetty to 12.0.5 [#129](https://github.com/thmarx/cms/issues/129)
* **MAINTENANCE** search-module, update lucene to 9.9.1 [#130](https://github.com/thmarx/cms/issues/130)

## 3.1.0
* **FEATURE** Layer for configuration abstraction [#125](https://github.com/thmarx/cms/issues/125)
* **FEATURE** Clean up template functions [#124](https://github.com/thmarx/cms/issues/124)
* **FEATURE** Logfiles per host [#93](https://github.com/thmarx/cms/issues/93)
* **FEATURE** Taxonomy for string values [#4](https://github.com/thmarx/cms/issues/4)
* **MAINTENANCE** Update markedjs to 11.1.0
* **FEATURE** Integrate modules framework for better customization

## 3.0.3
* **BUGFIX** If content file is not found, no 404 is thrown
* **BUGFIX** Navigation function throws exception if index.md is missing

## 3.0.2
* **BUGFIX** Fix navigation issue when using pebble engine

## 3.0.1
* **BUGFIX** Preview parameter was not added to generated urls [#123](https://github.com/thmarx/cms/issues/123)

## 3.0.0

* **BREAKING CHANGE** Refactoring meta fields, forces to update projects
* **BREAKING CHANGE** ContentQuery legacy methods removed, for pagination use _page_ method
* **FEATURE** MediaService to access meta data of medias
* **FEATURE** Pages can redirect to external web sites
* **FEATURE** unpublish_date meta field
* **FEATURE** Detected and reload site properties
* **FEATURE** Support for different content types to generate *json* or *html*, default content type is *text/html*
* **FEATURE** New site property *content.type* to set default content type
* **FEATURE** New meta field *excerpt* to add custom excerpt
* **FEATURE** Add depth to navigation function to easier build subnavigations

### Migration
#### Renamed MetaFields
published -> publish_date
draft -> published (attention: values are inverted)
#### ContentQuery
.get(offset, size) -> .page(page_number, page_size)

## 2.16.0
* **FEATURE** Enable gfm and anchors in markedjs module [#113](https://github.com/thmarx/cms/issues/113)
* **BUGIX** Multiple bugs with visibility of pages [#114](https://github.com/thmarx/cms/issues/114)
* **BUGIX** Remove debug logging from markedjs module [#111](https://github.com/thmarx/cms/issues/111)
* **BUGIX** Error accessing theme modules [#112](https://github.com/thmarx/cms/issues/112)

## 2.15.0
* **FEATURE** SEO module to auto generate xml sitemap [#98](https://github.com/thmarx/cms/issues/98)

## 2.14.0
* **FEATURE** Update markedjs module to version 11.0.0 [#105](https://github.com/thmarx/cms/issues/105)
* **FEATURE** Allow to config multiple hostnames per site [#103](https://github.com/thmarx/cms/issues/103)
* **FEATURE** Preview mode added [#102](https://github.com/thmarx/cms/issues/102)

## 2.13.2

* **BUGFIX** JettyDefaultHandler does not call _callback.succeeded()_

## 2.13.1

* **BUGFIX** Wrong method overloading in query causes pebble engine exception

## 2.13.0

* **FEATURE** GIT integration to add a deployment workflow [#100](https://github.com/thmarx/cms/issues/100)
* **FEATURE** Deliver static files like **robots.txt** [#97](https://github.com/thmarx/cms/issues/97)
* **FEATURE** Make excerpt parameterized [#96](https://github.com/thmarx/cms/issues/96)
* **FEATURE** Exclude content from search via meta field [#95](https://github.com/thmarx/cms/issues/95)
* **BUGFIX** Fix issue while loading media formats [#94](https://github.com/thmarx/cms/issues/94)

## 2.12.0

* **FEATURE** Temp directory per site [#89](https://github.com/thmarx/cms/issues/89)
* **FEATURE** Cropping of images [#88](https://github.com/thmarx/cms/issues/88)
* **FEATURE** Extend query function to get count [#86](https://github.com/thmarx/cms/issues/86)

## 2.11.0

* let query return excerpt from content

## 2.10.0

* Switch to GPLv3 [#82](https://github.com/thmarx/cms/issues/82)
* **BUGIX** Fix issue with query function when using pebble template engine [#81](https://github.com/thmarx/cms/issues/81)
* **FEATURE** Extend query function to allow le, leq, gt and gte [#79](https://github.com/thmarx/cms/issues/79)

## 2.9.1

* Bugfix for nodelist function if current not is not on root level and use relative start path

## 2.9.0

* error, if theme template engine does not match site template engine [#76](https://github.com/thmarx/cms/issues/76)
* add query function to template to select nodes, secondary index is experimental [#37](https://github.com/thmarx/cms/issues/37)

## 2.8.0

* update markedjs markdown renderer to version 10 [#74](https://github.com/thmarx/cms/issues/74)

## 2.7.0

* theme support [#12](https://github.com/thmarx/cms/issues/12)
* legacy server implementation removed [#68](https://github.com/thmarx/cms/issues/68)
