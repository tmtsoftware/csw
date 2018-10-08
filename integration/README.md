## Integration Test Runner Guide

### Prerequisites
* Packaged and unzipped Integration project
    * To package and unzip integration project, execute `./universal_package.sh` scripts from root level 


### How To Run
From root level (csw) run below shell scripts->
1. `./integration/scripts/runner.sh` => This will start TestApp.scala and execute LocationServiceIntegrationTest
2. `./integration/scripts/multiple_nic_test.sh` => This will start TestMultipleNicApp.scala and execute LocationServiceMultipleNICTest
