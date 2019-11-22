# asciidoc2confluence
Asciidoc to confluence converter & uploader

## Motivation
Motivation for this project has rosen from the need to convert AsciiDoc documents with code blocks to confluence. As standard asciidoctor behavior does not let to insert code blocks in format of confluence code macro, we need to postprocess asciidoctor output.

## Publishing to Confluence
To publish a document to confluence server its title, spaceKey and (optionally) parent page should be known.
Parser takes these parameters from comments in input file:

```
// :DOCUMENT-SPACE-KEY: TEST-SPACE-KEY
// :DOCUMENT-TITLE: Test Document Title
// :DOCUMENT-PARENT: Test Parent Page
```

- If document with given title already exists, it will be overwritten
- If document with "DOCUMENT-PARENT" title exists, new document will be added to its children

Also confluence server credentials should be provided to program:

```
  --url=http://localhost:8090 --user=login --pass=pa$$w0rd
```
