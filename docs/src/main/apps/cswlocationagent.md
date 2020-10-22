# csw-location-agent

A utility application that starts a given external (non-CSW) program, registers a comma separated list of services with the Location Service, and unregisters them when the program exits.

## Prerequisite

- Location server should be running.

## Command line parameter options

* **`--prefix`** Required: The prefix (or prefixes, separated by comma) used to register the application (also root name in config file).
* **`--command`** is an optional parameter. The command that starts the target application. Use use %port to specify the port number. If the parameter is not provided, the value $name.command from the config file will be picked up. If the value in the config file is not found, the service names provided will be registered with Location Service.
* **`--port`** the optional port number the application listens on (default: use the value of $name.port from the config file, or use a random, free port.)
* **<app-config>** an optional config file in HOCON format. Will be automatically picked based on --names parameter (Options specified as: $name.command, $name.port, etc.)
* **`--delay`** the number of milliseconds to wait for the app to start before registering itself with the Location Service (default: 1000)
* **`--http`** is an optional parameter. To register services as HTTP with the provided path. (default: none, i.e Services will be registered as Tcp)
* **`--publicNetwork`** is an optional parameter. To register service using public network IP. (default: false, i.e register service using private network IP)
* **`--no-exit`** For testing: prevents application from exiting after running command
* **`--help`** Prints the help message.
* **`--version`** Prints the version of the application.

## Running latest release of location-agent using Coursier

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
### 2. Install location-agent app

Following command creates an executable file named location-agent in the default installation directory.

```bash
cs install location-agent:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    location-agent:<version | SHA>
```
Note: If you don't provide the version or SHA in above command, `location-agent` will be installed with the latest tagged binary of `csw-location-agent`

### 3. Examples
 

```bash
//cd to installation directory
cd /tmt/apps

./location-agent --prefix "csw.redis" --command "redis-server /usr/local/etc/redis.conf" --port 6379
```
Application will start a Redis server on port 6379 (the default Redis port) and will register a TcpConnection for it with the Location Service 

 
```bash
//cd to installation directory
cd /tmt/apps

./location-agent --prefix "CSW.foo" --command "sleep 30"
```
Application will sleep for 30 seconds. It will be registered as a service named `CSW.foo` on a random port with the Location Service. After the sleep is over in 30 seconds, it will unregister the `CSW.foo` service.


```bash
//cd to installation directory
cd /tmt/apps

./location-agent --prefix "CSW.myHttpServiceAsTcp" --command "python -m SimpleHTTPServer 8080" --port 8080
```  
Application will start a simple HTTP service on port 8080. This will register `CSW.myHttpServiceAsTcp` as a TCP service with the Location Service.

 
```bash
//cd to installation directory
cd /tmt/apps

./location-agent --prefix "CSW.myHttpServiceAsHttp" --command "python -m SimpleHTTPServer 8080" --port 8080 --http "path"
```  
Application will start a simple HTTP service on port 8080. This will register `CSW.myHttpServiceAsHttp` as a HTTP service with the provided path with the Location Service. It will register its private network ip with the Location Service.


```bash
//cd to installation directory
cd /tmt/apps

./location-agent --prefix "CSW.myHttpServiceAsHttp" --command "python -m SimpleHTTPServer 8080" --port 8080 --http "path" --publicNetwork
```  
Application will start a simple HTTP service on port 8080. This will register `CSW.myHttpServiceAsHttp` as a HTTP service with the provided path with the Location Service. It will register its public network ip with the Location Service.
 

```bash
//cd to installation directory
cd /tmt/apps

./location-agent --help
```  
Prints help message


```bash
//cd to installation directory
cd /tmt/apps

./location-agent --version
```  
Prints application version


## Running latest master of location-agent on developer machine

The CSW Location Agent application can be installed as binaries or constructed from source. To download the application,
go to the [CSW Release page](https://github.com/tmtsoftware/csw/releases) and follow instructions.

To run the latest master on dev machine  either use the command `sbt run`, or the command `sbt publishLocal` followed by `cs launch location-agent:0.1.0-SNAPSHOT`.

Command line parameters can also be passed while launching SNAPSHOT version using coursier.

```bash
  // run location agent using coursier
  cs launch location-agent:0.1.0-SNAPSHOT -- --prefix "CSW.foo" --command "sleep 5"
```
