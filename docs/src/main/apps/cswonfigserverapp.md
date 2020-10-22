# csw-config-server

A HTTP server application that hosts the Configuration Service.
  
## Prerequisites

- Location server should be running.
- CSW AAS should be running.

@@@ note

Before running `config-server`, `sbt "csw-services/run start -k"` command can be run to start the location service along with the aas/auth service.

@@@

## Command line parameter options

* **`--initRepo`** is an optional parameter. When supplied, the server will try to initialize a repository if it does not exist.
* **`--port`** is an optional parameter. When specified, the HTTP server will start on this port. Default will be 4000.
* **`--help`** prints the help message.
* **`--version`** prints the version of the application.

## Running latest release of config-server using Coursier

### 1. Add TMT Apps channel to your local Coursier installation using below command

Channel needs to be added to install application using `cs install`

For developer machine setup,

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

For production machine setup,

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```
### 2. Install config-server app

Following command creates an executable file named lconfig-server in the default installation directory.

```bash
cs install config-server:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    config-server:<version | SHA>
```
Note: If you don't provide the version or SHA in above command, `config-server` will be installed with the latest tagged binary of `csw-config-server`

## Examples


```bash
//cd to installation directory
cd /tmt/apps

./config-server --initRepo
```  

Start an HTTP server on default port 4000. Initialize the repository if it does not exist and register it with the Location Service
 

```bash
//cd to installation directory
cd /tmt/apps

./config-server --initRepo --port 4001
```  

Start an HTTP server on port 4001. Initialize the repository if it does not exist and register it with the Location Service 


```bash
//cd to installation directory
cd /tmt/apps

./config-server --help
```  

Prints help message


```bash
//cd to installation directory
cd /tmt/apps

./config-server --version
```   

Prints application version

## Running latest master of location-agent on developer machine

The CSW Config Server application can be installed as binaries or constructed from source. To download the application,
go to the [CSW Release page](https://github.com/tmtsoftware/csw/releases) and follow instructions.

To run the latest master on dev machine  either use the command `sbt run`, or the command `sbt publishLocal` followed by `cs launch config-server:0.1.0-SNAPSHOT`.

Command line parameters can also be passed while launching SNAPSHOT version using coursier.

```bash
  // run location agent using coursier
  cs launch config-server:0.1.0-SNAPSHOT -- --initRepo
```
