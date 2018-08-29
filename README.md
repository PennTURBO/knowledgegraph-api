# Dashboard REST API #

REST API for communication between various TURBO components

## Build & Run ##

```sh
$ cd Dashboard
$ sbt
> jetty:start
```

Once server is started, send medication mapping input strings to "http://localhost:8080/medications" as JSON.
