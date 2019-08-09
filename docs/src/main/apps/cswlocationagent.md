# csw-location-agent

A utility application that starts a given external (non-CSW) program, registers a comma separated list of services with the Location Service, and unregisters them when the program exits.

## Command line parameter options

* **`--names`** is a required parameter. It needs to be a list of comma separated service names without a space after comma.
* **`--command`** is an optional parameter. The command that starts the target application. Use use %port to specify the port number. If the parameter is not provided, the value $name.command from the config file will be picked up. If the value in the config file is not found, the service names provided will be registered with Location Service.
* **`--port`** the optional port number the application listens on (default: use the value of $name.port from the config file, or use a random, free port.)
* **<app-config>** an optional config file in HOCON format. Will be automatically picked based on --names parameter (Options specified as: $name.command, $name.port, etc.)
* **`--delay`** the number of milliseconds to wait for the app to start before registering itself with the Location Service (default: 1000)
* **`--http`** is an optional parameter. To register services as HTTP with the provided path. (default: none, i.e Services will be registered as Tcp)
* **`--no-exit`** For testing: prevents application from exiting after running command
* **`--help`** Prints the help message.
* **`--version`** Prints the version of the application.

## Examples

1. 
```
csw-location-agent --name "redis" --command "redis-server /usr/local/etc/redis.conf" --port 6379
```  
Application will start a Redis server on port 6379 (the default Redis port) and will register a TcpConnection for it with the Location Service 

2. 
```
csw-location-agent --name "foo" --command "sleep 30"
```
Application will sleep for 30 seconds. It will be registered as a service named `foo` on a random port with the Location Service. After the sleep is over in 30 seconds, it will unregister the `foo` service.

3. 
```
csw-location-agent --name "myHttpServiceAsTcp" --command "python -m SimpleHTTPServer 8080" --port 8080
```  
Application will start a simple HTTP service on port 8080. This will register `myHttpServiceAsTcp` as a TCP service with the Location Service.

4. 
```
csw-location-agent --name "myHttpServiceAsHttp" --command "python -m SimpleHTTPServer 8080" --port 8080 --http "path"
```  
Application will start a simple HTTP service on port 8080. This will register `myHttpServiceAsHttp` as a HTTP service with the provided path with the Location Service.

5. 
```
csw-location-agent --help
```  
Prints help message

6. 
```
csw-location-agent --version
```  
Prints application version

@@@ note

Before running `csw-location-agent`, make sure that `csw-location-server` is running on the local machine at `localhost:7654`, since the
location agent uses a local HTTP location client which expects the Location Server to be running locally.

@@@
