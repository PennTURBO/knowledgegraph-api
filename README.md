# Dashboard REST API #

REST API for communication between various TURBO components

## Build & Run ##

```sh
$ cd Dashboard
$ sbt
> jetty:start
```

For free text lookup, send POST JSON to "http://localhost:8080/medications/findOrderNamesFromInputString"

Example input for free text lookup:

    {"searchTerm":"analgesic"}

For URI lookup, send POST JSON to "http://localhost:8080/medications/findOrderNamesFromInputURI"

    {"searchTerm":"http://purl.obolibrary.org/obo/CHEBI_35480"}

Note that when running from SBT the default port is 8080, as a precompiled .jar the default port is 8089.

See dashboardApiDocs.raml for more explicit documentation.
