# asciidoc2confluence
Asciidoc to confluence converter & uploader


## Motivation
Motivation for this project has risen from the need to convert AsciiDoc documents with code blocks to confluence. 
As standard asciidoctor behavior does not let to insert code blocks in format of confluence code macro, we need 
to postprocess asciidoctor output.


## Publishing to Confluence
To publish a document to confluence server its meta-parameters should be known.
Parser takes these parameters from comments in input file:

```
// :DOCUMENT-TITLE: Document title [mandatory header]
// :DOCUMENT-TITLE-OLD: Old document title (in case if document is being renamed) [optional header]
// :DOCUMENT-PARENT: Parent page title [optional header]
// :DOCUMENT-TAGS: tag1, long tag 2, tag3 [optional header]
```

... and from command-line argument `--space=CONFLUENCE_SPACE_KEY`.  

- If document with `DOCUMENT-TITLE` title already exists, its content/title/tags will be updated
- If document with `DOCUMENT-TITLE-OLD` title exists, it will be removed to `DOCUMENT-TITLE`
- If document with `DOCUMENT-PARENT` title exists, new document will be added to its children

Also confluence server credentials should be provisioned to program:

```
  --url=http://localhost:8090 --user=login --pass=pa$$w0rd
```

If confluence credentials are not passed to program, converted document(s) will be printed to STDOUT.

For debugging purposes you can print converted document onto STDOUT in case of publishing error. 
Use `--debug` argument for this.

*NOTE*: From now on `DOCUMENT-SPACE` parameter is not used to specify document's space in confluence. This means, that 
confluence space to work with should be provided with `--space` command-line argument. Which, in turn, means that all 
the input files should go to the same confluence space.  


## Cleaning up Confluence space(s)
You can clean up _space_ in confluence before publishing pages. For this use `--clean=SPACE1,SPACE2` command-line 
argument with comma-separated list of space keys to be cleaned up. If this argument is passed to program all the 
_unprotected_ pages in mentioned spaces will be removed before publishing stage. Only pages tagged with _protected_ 
label(s) will be kept. Protected labels are set in configuration file and can be overridden with environment variable 
`PROTECTED_LABELS`. Like this:

```
export PROTECTED_LABELS=protected_label_1,protected_label_2
``` 
Also you can forcefully clean up confluence space(s), removing all the pages (*even _protected_*). 
For this use `--force` command line argument. 


## Converting documents
The data for program can be provisioned with two ways:
- single file (`--input=<fileName>`)

    In this case only one file will be processed.
    
- directory (`--dir=<directory>`)
    
    In this case directory will be processed recursively. All the *.adoc and *.asciidoc files will be converted (and, 
    if related arguments are set, published to confluence server). Subdirectories are being processed in such a way 
    that at first all files from a directory are being processed, then all the subdirectories are being processed. 
    This means that all the higher-level pages should be placed in higher-level directories, so that parent pages would
    be created before related siblings.
    
If `--dir` and `--input` arguments are used together, only `--dir` key will be considered.


## Tagging documents
If `DOCUMENT-TAGS` header is set for document, related page on confluence will be labeled with configured tags. Tags 
should be separated with commas. Spaces in multi-word tags will be converted to underscores ('_'), as confluence does 
not support multi-word labels 
(see [here](https://confluence.atlassian.com/jirakb/creating-multiple-word-labels-779160786.html)).


## Removing stale documents
After being published to confluence server some documents can be later removed from document repository. Such documents 
are found during directory processing and removed from confluence server.
 
(Program gets a list of all the documents in local repository and all the documents from confluence server [for given 
`space`]. All the documents found on a server and not found in local repository will be removed from server.) 


## Disable document publishing
For testing purposes or during documentation preparation you may need to prevent existing document
from being published to confluence server. For this you can use `DOCUMENT-HIDDEN` header like this:
```
// prevents document from publication
// :DOCUMENT-HIDDEN:    
```

```
// prevents document from publication
// :DOCUMENT-HIDDEN: true    
```

```
// allows document publication
// :DOCUMENT-HIDDEN: false
```
If document is already published and then marked with `DOCUMENT-HIDDEN` flag, it will be removed from server.


## Links processing
Program tries to convert links to native confluence format. The links in source AsciiDoc can be of the 
following formats:

* `http://link.to.some.internet.site[link display text]`

    In this case ordinary link to external resource will be added to confluence page.
    
* `link:CONFLUENCESPACE:Page+title[link display text]`
    
    In this case there will be created confluence link, pointing to page titled `Page title` in space `CONFLUENCESPACE` 
    on the same confluence server.
    
* `link:Page+title[link display text]`

    In this case there will be created confluence link, pointing to page titled `Page title` in the same space, which the
    page being processed belongs to.
    
*NOTE*: When creating links in `link:` format, spaces in page title should be replaces with '+' symbols in 
source AsciiDoc. 


## Table Of Contents
To insert TOC macro in resulting confluence page use following syntax:

```
// :DOCUMENT-SPACE-KEY: TESTSPACE
// :DOCUMENT-TITLE: Main page

= Title
:hardbreaks:
:toc:

{zwsp}

... the rest of document ...
```

## Page Tree
You can insert confluence page tree macro into source asciidoc files to be rendered to confluence 
[page tree](https://confluence.atlassian.com/display/CONF55/Page+Tree+Macro). Use following syntax:

```
// pagetree::Root+page+name[]
```

## Children Display 
You can insert confluence children display macro into source asciidoc files to be rendered to confluence 
[children display](https://confluence.atlassian.com/display/CONF55/Children+Display+Macro). Use following syntax:

```
// children::Parent+page+name[]
```

## Math formula support
Formula support is implemented with help of [MathJax plugin](https://marketplace.atlassian.com/apps/1217196/mathjax?hosting=server&tab=support).
Formulas can be inline `\(\sum_{i=0}^n i^2 = \frac{(n^2+n)(2n+1)}{6}\)` or block `\[\sum_{i=0}^n i^2 = \frac{(n^2+n)(2n+1)}{6}\]`.
By default block-formulas are aligned to center. To align them left or right you need to add following definitions to global Confluence CSS: 
```
.math-left .mjx-chtml.MJXc-display {
    text-align: left !important;
 }
.math-right .mjx-chtml.MJXc-display {
    text-align: right !important;
 }
```
Then you can use `\l` or `\r` in start of formula definition in source asciidoc file:
`\[\l\sum_{i=0}^n i^2 = \frac{(n^2+n)(2n+1)}{6}\]` or `\[\r\sum_{i=0}^n i^2 = \frac{(n^2+n)(2n+1)}{6}\]`

