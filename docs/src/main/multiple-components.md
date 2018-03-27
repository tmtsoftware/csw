# Multiple Components

Deploying multiple components (in Container), and having them communicate with each other.

## Creating an Assembly

## Loading Configuration Files

## Tracking Dependencies

### Other useful Location Service basics (Location/Connection types)
### ComponentInfo fields
### onLocationTrackingEvent Handler

## Sending Commands

NOTE: I put moved the line below from creating-components -- JLW
The creation of a`CommandService` instance and its usage can also be referred [here](https://tmtsoftware.github.io/csw-prod/command.html#commandservice).



### Tracking Long Running Commands

### Matchers

### PubSub Connection

## Deploying Multiple Components in a Container

## Running components in a Container


NOTE: the following was moved from the getting starting page. I thought it was more appropriate here, 
but I just cut/paste for now, expecting some of this info will be integrated into this page. -- JLW
## Deploying and Running Components

### Pre-requisite

`galil-deploy` project contains applications (ContainerCmd and HostConfig) to run your components, make sure you add necessary dependencies in `galil-deploy` project.


### Run
As seen above `galil-deploy` depends on `galil-assembly` and `galil-hcd`, now if you want to start these Assembly and HCD, follow below steps:

 - Run `sbt galil-deploy/universal:packageBin`, this will create self contained zip in `galil-deploy/target/universal` directory
 - Unzip generated zip file and enter into `bin` directory
 - You will see four scripts in `bin` directory (two bash scripts and two windows scripts)
 - If you want to start multiple containers on a host machine, follow this guide @ref:[here](apps/hostconfig.md#examples)
 - If you want to start multiple components in container mode or single component in standalone mode, follow this guide @ref:[here](framework/deploying-components.md)
 - Example to run container:    `./galil-container-cmd-app --local ../../../../galil-deploy/src/main/resources/GalilAssemblyContainer.conf`
 - Example to run host config:  `./galil-host-config-app --local ../../../../galil-deploy/src/main/resources/GalilHostConfig.conf -s ./galil-container-cmd-app`

@@@ note { title=Note }

CSW Location Service cluster seed must be running, and appropriate environment variables set to run apps.
See https://tmtsoftware.github.io/csw-prod/apps/cswclusterseed.html.

@@@
