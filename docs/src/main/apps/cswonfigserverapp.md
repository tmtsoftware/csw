# csw-config-server

A HTTP server application that hosts Configuration Service.
  
## Prerequisites
The HTTP server hosting Configuration Service needs to be part of csw-cluster so that it can be consumed by other components.
A required check before starting configuration service is to ensure the csw-cluster is setup and location server are available.
Kindly refer to @ref:[CSW Location Server](../apps/cswlocationserver.md) for more information about location server setup.

## Command line parameter options

* **`--initRepo`** is an optional parameter. When supplied, server will try to initialize a repository if it does not exist.
* **`--port`** is an optional parameter. When specified the HTTP server will start on this port. Default will be 4000.
* **`--help`** prints the help message.
* **`--version`** prints the version of the application.

## Examples

1. 
```
csw-config-server --initRepo
```  
Start HTTP server on default port 4000. Initialize repository if it does not exist and register it with Location Service
 
2. 
```
csw-config-server --initRepo --port 4001
```  
Start HTTP server on port 4001. Initialize repository if it does not exist and register it with Location Service 

3. 
```
csw-config-server --help
```  
Prints help message

4. 
```
csw-config-server --version
```    
Prints application version

@@@ note

Before running `csw-config-server`, make sure that `csw-location-server` is running on local machine at `localhost:7654`.
As config server uses local HTTP location client which expect location server running locally.

@@@
