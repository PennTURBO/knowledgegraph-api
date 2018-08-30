# Dashboard REST API #

REST API for communication between various TURBO components

## Build & Run ##

```sh
$ cd Dashboard
$ sbt
> jetty:start
```

Once server is started, send medication mapping input strings to "http://localhost:8080/medications" as JSON.

Example input:

    val jsonString = """
    [
        {
            "fullName": "\"INSULIN ASPART 100 UNIT/ML SC SOLN\"",
            "fullName": "\"ONDANSETRON HCL 4 MG/2ML INJECTION SOLN\"",
            "fullName": "\"sodium chloride 0.9% -\""
        }
    ]
    """


