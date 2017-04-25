# csw-config-server

A HTTP server application that hosts configuration service.
  
## Prerequisites
The HTTP server hosting Configuration service, needs to be part of csw-cluster, so that it can be consumed by other components.
A required check before starting configuration service, is to ensure cluster is set-up and seed nodes are available.
Kindly refer to this [cluster-seed setup](https://tmtsoftware.github.io/csw-prod/apps/cswclusterseed.html).

## Command line parameter options

* **--initRepo** It's an optional parameter. When supplied, server will try to initialize a repository if it does not exist.
* **--port** It's an optional parameter. When specified the HTTP server will start on this port. Default will be 4000.
* **--help** Prints the help message.
* **--version** Prints the version of the application.

## Examples

**Example:** csw-config-server --initRepo  
**Explanation:** Start HTTP server on default port 4000. Initialize repository if it does not exist and register it with LocationService
 
**Example:** csw-config-server --initRepo --port 4001  
**Explanation:** Start HTTP server on port 4001. Initialize repository if it does not exist and register it with LocationService 

**Example:** csw-config-server --help  
**Explanation:** Prints help message

**Example:** csw-config-server --version  
**Explanation:** Prints application version