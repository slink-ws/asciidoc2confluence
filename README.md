# asciidoc2confluence
Asciidoc to confluence converter & uploader


## Motivation
Motivation for this project has rosen from the need to convert AsciiDoc documents with code blocks to confluence. As standard asciidoctor behavior does not let to insert code blocks in format of confluence code macro, we need to postprocess asciidoctor output.


## Publishing to Confluence
To publish a document to confluence server its meta-parameters should be known.
Parser takes these parameters from comments in input file:

```
// :DOCUMENT-SPACE-KEY: CONFLUENCE-SPACE-KEY [mandatory header]
// :DOCUMENT-TITLE: Document title [mandatory header]
// :DOCUMENT-TITLE-OLD: Old document title (in case if document is being renamed) [optional header]
// :DOCUMENT-PARENT: Parent page title [optional header]
// :DOCUMENT-TAGS: tag1, long tag 2, tag3 [optional header]
```

You can override document's SPACE-KEY parameter, if you want to publish document to another space for testing purposes.
For this use `--space=ANOTHERSPACE` command-line argument.  

- If document with `DOCUMENT-TITLE` title already exists, it will be overwritten
- If document with `DOCUMENT-TITLE-OLD` title exists, it will be removed
- If document with `DOCUMENT-PARENT` title exists, new document will be added to its children

Also confluence server credentials should be provisioned to program:

```
  --url=http://localhost:8090 --user=login --pass=pa$$w0rd
```

If confluence credentials are not passed to program, converted document(s) will be printed to STDOUT.


## Converting documents
The data for program can be provisioned with two ways:
- single file (`--input=<fileName>`)

    In this case only one file will be processed.
    
- directory (`--dir=<directory>`)
    
    In this case directory will be processed recursively. All the *.adoc and *.asciidoc files will be converted (and, 
    if related argumtnts are set, published to confluence server). Subdirectories are being processed in such a way 
    that at first all files from a directory are being processed, then all the subdirectories are being processed. 
    This means that all the higher-level pages should be placed in higher-level directories, so that parent pages would
    created before related siblings.
    
If `--dir` and `--input` arguments are used together, only `--dir` key will be considered.


## Tagging documents
If `DOCUMENT-TAGS` header is set for document, related page on confluence will be labeled with configured tags. Tags 
should be separated with commas. Spaces in multi-word tags will be converted to underscores ('_'), as confluence does 
not support multi-word labels 
(see [here](https://confluence.atlassian.com/jirakb/creating-multiple-word-labels-779160786.html)).


## Links processing
Program tries to convert links to native confluence format. The links in source AsciiDoc can be of the 
following formats:

* `http://link.to.some.internet.site[link display text]`

    In this case ordinary link to external resource will be added to confluence page.
    
* `link:CONFLUENCESPACE:Page+title[link display text]`
    
    In this case there will be created confluence link, pointing to page titled `Page title` in space `CONFLUENCESPACE` 
    on the same confluence serve
    
* `link:Page+title[link display text]`

    In this case there will be created confluence link, poiting to page titles `Page title` in the same space, which is
    used for the page being processed.
    
NOTE: When creating links in `link:` format, spaces in page title should be replaces with '+' symbols in 
source AsciiDoc 


## Table Of Contents
To insert TOC macro in resulting confluence page use following syntax:

```asciidoc
// :DOCUMENT-SPACE-KEY: TESTSPACE
// :DOCUMENT-TITLE: Main page

= Title
:hardbreaks:
:toc:

{zwsp}

... the rest of document ...
```
