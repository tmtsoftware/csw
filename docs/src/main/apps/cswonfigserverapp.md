# csw-config-server

A HTTP server application that hosts the Configuration Service.
  
## Prerequisites

- Location Service should be running
- CSW AAS should be running

@@@ note

This page assumes that you have already installed and setup @ref:[coursier](csinstallation.md) { open=new }

@@@

## Install csw-config-server app

Following command creates an executable file named csw-config-server in the default installation directory.

```bash
cs install csw-config-server:<version | SHA>
```

Note: If you don't provide the version or SHA in above command, `csw-config-server` will be installed with the latest tagged binary of `csw-config-server`

## Command line parameter options

* **`--initRepo`** is an optional parameter. When supplied, the server will try to initialize a repository if it does not exist.
* **`--port`** is an optional parameter. When specified, the HTTP server will start on this port. Default will be 4000.
* **`--help`** prints the help message.
* **`--version`** prints the version of the application.

## Examples

1.  Start an HTTP server on default port 4000. Initialize the repository if it does not exist and register it with the Location Service.
    ```bash
    csw-config-server --initRepo
    ```  

2.  Start an HTTP server on port 4001. Initialize the repository if it does not exist and register it with the Location Service.     
    ```bash
    csw-config-server --initRepo --port 4001
    ```  

3.  Prints help message
    ```bash
    csw-config-server --help
    ```  

4. Prints application version
    ```bash
    csw-config-server --version
    ```
