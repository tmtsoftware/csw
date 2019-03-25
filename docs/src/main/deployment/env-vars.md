# Environment variables 

List of environment variables which needs to be set for using different Csw services.

| Variable       | Dev       | Prod      | Needs to set before   | Description               |
| :------------: |:--------: | :-------: | :------------------:  | :------------------------ |
| clusterSeeds   | Mandatory | Mandatory | starting csw services | The Host and port of the seed nodes of cluster, Ex. clusterSeeds=“127.0.0.1:3552, 127.0.0.2:3552”. |
| INTERFACE_NAME  | Mandatory | Mandatory | starting csw services | Network interface in which AKKA cluster is formed, Ex. INTERFACE_NAME=en0. |
| clusterPort    | Optional  | Mandatory | starting location service | Port on which location service will start. For dev default is random portPort. |
| managementPort | Optional  | Optional  | starting location service |  Port on which Akka provided cluster management service will start (if not provided service won’t start) |
| cswAdminPrefix | Optional  | Optional  | firing unlock command | Needed to give admin the capability of unlocking the locked components. (If not set, command will result in no-op) |
| TMT_LOG_HOME   | Optional  | Mandatory | starting any Csw service | Base path of directory to hold log files from TMT apps. (Log files will be generated only if file appender is activated) |
| dbReadUsername & dbReadPassword | Optional | Mandatory (for components using DB service) | starting DB service | Needed to create connection with Database service with read access. |
| dbWriteUsername & dbWritePassword | Optional | Mandatory (for components using DB service) | starting DB service | Needed to create connection with Database service with write access. |
| dbAdminUsername & dbAdminPassword | Optional | Mandatory (for components using DB service) | starting DB service | Needed to create connection with Database service with admin access. |

#### Terminologies
 
* Dev - For starting services using `./csw-services.sh`
* Prod - For starting services by ways other than `./csw-services.sh`