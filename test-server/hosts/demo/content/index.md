---
template: start.html
parent:
  text: another text for the meta attribute , seems to work
range_test: 42
choose_color: green
radioed: form
author: thorsten
count: 20
description: Thats awesome
taxonomy:
  tags:
  - kleidung
  - hoodies_sweatshirts
  - schuhe
  - kinderkleidung
  - Small Test
  - New tag
title: Startpage
media_url: images/fff.png
features:
- export
search:
  index: false
background_color: '#c2e0c6'
translations:
  de: /
checked:
- farbe
unpublish_date: null
seo:
  description: hier kommt die beschreibung
linked_page: /about
publish_date: null
selected: form
status: published
object:
  values:
  - title: Test 1 and more
    description: desc 1 - 2
  - title: Test 2 update
  - title: test
    description: blub
    features: search
---

# Demo Project

![TestBild!](/media/images/test.jpg?format=small)

That's a demo page with some extra features to show the manager application!

Hello world 2!

Here some content!

Hello: [[cms:username]][[/cms:username]]  
Theme: [[ext:theme_name]][[/ext:theme_name]]

[about](/about)

[this is a new page](/this-is-a-new-page)


```java
// its a comment
System.out.println("Hello world!");
```

### say hello
[[ext:say_hello name="CondationCMS" /]]


### test ShortCode with content
---
[[ext:bold_content]]This content will be bold[[/ext:bold_content]]
---


### example from module
[[ext:example /]]
