# Environment variables 

List of environment variables which needs to be set for using different Csw services.

| Variable       | Dev       | Prod      | Needs to set before   | Description               |
| :------------: |:--------: | :-------: | :------------------:  | :------------------------ |
| CLUSTER_SEEDS   | Optional | Mandatory | starting csw services | The Host and port of the seed nodes of cluster, Ex. CLUSTER_SEEDS=“192.168.1.21:3552, 192.168.1.22:3552”. |
| INTERFACE_NAME  | Optional | Mandatory | starting csw services | Network interface in which the Akka cluster is formed, Ex. INTERFACE_NAME=en0. |
| PUBLIC_INTERFACE_NAME  | Optional | Mandatory (for services exposed to Outside network) | starting csw services | Network interface in which services exposed to Outside network are registered, Ex. INTERFACE_NAME=en1. |
| MANAGEMENT_PORT | Optional  | Optional  | starting location service |  Port on which the Akka provided cluster management service will start (if not provided service won’t start) |
| TMT_LOG_HOME   | Optional  | Mandatory | starting any Csw service | Base path of the directory to hold log files from TMT apps. (Log files will be generated only if file appender is enabled) |
| DB_READ_USERNAME & DB_READ_PASSWORD | Optional | Mandatory (for components using DB service) | starting DB service | Needed to create connection with the Database Service with read access. |

@@@ note
The environment variable names for database write access and database admin access are not mentioned here because their names will be
specific to their corresponding databases.
@@@

#### Terminologies
 
* Dev - For starting services using `./csw-services.sh`
* Prod - For starting services by ways other than `./csw-services.sh`
* Outside network - Refer @ref:[Network Topology](network-topology.md).


