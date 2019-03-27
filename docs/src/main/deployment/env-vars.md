# Environment variables 

List of environment variables which needs to be set for using different Csw services.

| Variable       | Dev       | Prod      | Needs to set before   | Description               |
| :------------: |:--------: | :-------: | :------------------:  | :------------------------ |
| CLUSTER_SEEDS   | Mandatory | Mandatory | starting csw services | The Host and port of the seed nodes of cluster, Ex. CLUSTER_SEEDS=“127.0.0.1:3552, 127.0.0.2:3552”. |
| INTERFACE_NAME  | Mandatory | Mandatory | starting csw services | Network interface in which AKKA cluster is formed, Ex. INTERFACE_NAME=en0. |
| CLUSTER_PORT    | Optional  | Mandatory | starting location service | Port on which location service will start. For dev default is random portPort. |
| MANAGEMENT_PORT | Optional  | Optional  | starting location service |  Port on which Akka provided cluster management service will start (if not provided service won’t start) |
| TMT_LOG_HOME   | Optional  | Mandatory | starting any Csw service | Base path of directory to hold log files from TMT apps. (Log files will be generated only if file appender is enabled) |
| DB_READ_USERNAME & DB_READ_PASSWORD | Optional | Mandatory (for components using DB service) | starting DB service | Needed to create connection with Database service with read access. |

@@@ note
The environment variable names for db write access and db admin access are not mentioned here because their names will be
specific to their corresponding databases
@@@

#### Terminologies
 
* Dev - For starting services using `./csw-services.sh`
* Prod - For starting services by ways other than `./csw-services.sh`