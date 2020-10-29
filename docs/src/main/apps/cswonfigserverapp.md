# csw-config-server

A HTTP server application that hosts the Configuration Service.
  
## Prerequisites

- Location server should be running.
- CSW AAS should be running.

@@@ note

Before running `config-server`, `sbt "csw-services/run start -k"` command can be run to start the location service along with the aas/auth service or
`cs launch csw-services -- start -k` command can also be used.

@@@

## Command line parameter options

* **`--initRepo`** is an optional parameter. When supplied, the server will try to initialize a repository if it does not exist.
* **`--port`** is an optional parameter. When specified, the HTTP server will start on this port. Default will be 4000.
* **`--help`** prints the help message.
* **`--version`** prints the version of the application.

## Running latest release of config-server using Coursier
@@@ note

This page assumes that you have already installed and set-up coursier : @ref:[coursier-installation](csinstallation.md) { open=new }.

@@@

### Install config-server app

Following command creates an executable file named lconfig-server in the default installation directory.

```bash
cs install config-server:<version | SHA>
```

Note: If you don't provide the version or SHA in above command, `config-server` will be installed with the latest tagged binary of `csw-config-server`

## Examples

1.  Start an HTTP server on default port 4000. Initialize the repository if it does not exist and register it with the Location Service.
    ```bash
    config-server --initRepo
    ```  

 
2.  Start an HTTP server on port 4001. Initialize the repository if it does not exist and register it with the Location Service.     
    ```bash
    config-server --initRepo --port 4001
    ```  

3.  Prints help message
    ```bash
    config-server --help
    ```  

4. Prints application version
    ```bash
    config-server --version
    ```   


## Running latest master of location-agent on developer machine

1.  To run the latest master on dev machine the command `sbt run` can be used.
    ```bash
      // run config-server using sbt
      sbt "csw-config-server/run"
    ```
    ```bash
       // run config-server using sbt with arguments
       sbt "csw-config-server/run --initRepo"
    ```


2. Alternatively the command `sbt publishLocal` followed by `cs launch config-server:0.1.0-SNAPSHOT` can be used.
   Command line parameters can also be passed while launching SNAPSHOT version using coursier.
    ```bash
      // run config-server using coursier
      cs launch config-server:0.1.0-SNAPSHOT -- --initRepo
    ``` 
