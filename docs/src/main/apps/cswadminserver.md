# csw-admin-server

A HTTP server application that supports changing/getting log level of component. 

## Prerequisites

A required check before starting csw-admin-server app is to ensure the csw-cluster is setup and location sever is available so that using HTTP location client, 
it can resolve components for which log level needs to be fetched/changed..
Kindly refer to @ref:[CSW Location Server](../apps/cswlocationserver.md) for more information about location server setup.

## Command line parameter options

* **`--port`** is an optional parameter. When specified the HTTP server will start on this port. Default is 7878.
* **`--locationHost`** is an optional parameter. Indicates the host address of machine where location server is running. Default is localhost.
* **`--help`** prints the help message.
* **`--version`** prints the version of the application.

## Examples

1. 
```
csw-admin-server
```  
Start HTTP server on default port 7878.

@@@ note
This requires that location server is running locally
@@@

2. 
```
csw-admin-server --locationHost 172.1.1.2
```  
Start HTTP server on default port 7878.

@@@ note
This indicates, location server is running on remote machine having ip address=`172.1.1.2`
@@@
 
3. 
```
csw-admin-server --port 8080
```  
Start HTTP server on port 8080.

4. 
```
csw-admin-server --help
```  
Prints help message

5. 
```
csw-admin-server --version
```    
Prints application version


 ## Accessing csw-admin-server routes via HTTP (curl)
 
 
1. Get LogMetadata : Returns current log level of component along with default, akka and slf4j log levels.
 
 * Request:
 ```
 curl -X GET 'http://10.131.21.184:7878/admin/logging/trombone-hcd-akka/level'
 ```
 
 * Response:
 ```
    {
        "defaultLevel": "DEBUG",
        "akkaLevel": "ERROR",
        "slf4jLevel": "INFO",
        "componentLevel": "DEBUG"
    }
 ```
 
2. Set Log Level : Sets the log level of component and returns with status code 200 OK
 
 * Request:
 ```
 curl -X POST 'http://10.131.21.184:7878/admin/logging/trombone-hcd-akka/level?value=DEBUG'
 ```

@@@ note

 Assuming csw-admin-server is running on IP 10.131.21.184 on port 7878.

@@@
