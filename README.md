# cms

cms is a simple java based flat file content management system.
see wiki for more information: [wiki](https://github.com/thmarx/cms/wiki)


# changelog

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
