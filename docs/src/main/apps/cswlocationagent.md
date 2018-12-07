# csw-location-agent

A utility application that starts a given external program, registers a comma separated list of services with the Location Service and unregisters them when the program exits.

## Command line parameter options

* **`--names`** is a required parameter. It needs to be a list of comma separated service names without a space after comma.
* **`--command`** is an optional parameter. The command that starts the target application. Use use %port to specify the port number. If parameter is not provided value $name.command from config file will be picked up. If value in config file is not found, the service names provided will be registered with Location Service.
* **`--port`** the optional port number the application listens on (default: use value of $name.port from config file, or use a random, free port.)
* **<app-config>** an optional config file in HOCON format. Will be automatically picked based on --names parameter (Options specified as: $name.command, $name.port, etc.)
* **`--delay`** the number of milliseconds to wait for the app to start before registering it with the Location Service (default: 1000)
* **`--http`** is an optional parameter. To register services as Http. (default: false, i.e Services will be registered as Tcp)
* **`--no-exit`** For testing: prevents application from exiting after running command
* **`--help`** Prints the help message.
* **`--version`** Prints the version of the application.

## Examples

1. 
```
csw-location-agent --name "redis" --command "redis-server /usr/local/etc/redis.conf" --port 6379
```  
Application will start redis server on port 6379(default redis port) and will register a TcpConnection for the same with Location Service 

2. 
```
csw-location-agent --name "foo" --command "sleep 30"
```
Application will sleep for 30 seconds. Then, will register a service named foo on a random port with Location Service. Once sleep is over after 30 seconds, will unregister foo service.

3. 
```
csw-location-agent --name "myHttpServiceAsTcp" --command "python -m SimpleHTTPServer 8080" --port 8080
```  
Application will start a simple HTTP service on port 8080. Then, will register myHttpServiceAsTcp as a TCP service with Location Service.

4. 
```
csw-location-agent --name "myHttpServiceAsHttp" --command "python -m SimpleHTTPServer 8080" --port 8080 --http
```  
Application will start a simple HTTP service on port 8080. Then, will register myHttpServiceAsHttp as a Http service with Location Service.

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

Before running `csw-location-agent`, make sure that `csw-location-server` is running on local machine at `localhost:7654`.
As location agent uses local HTTP location client which expect location server running locally.

@@@
