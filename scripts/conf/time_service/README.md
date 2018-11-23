# To run Time Service tests

Execute the following command

> docker run --privileged -v ~/Projects/tmt/csw:/csw twtmt/time-service

`--privileged` : Needed for sudo access since the tests try to set the kernel parameter for TAI offset

`-v src:dest` : Needed for mounting `csw` project on the container. Change the src to `csw` project's location on your machine.

`twtmt/time-service` : This is the docker image built on top of CentOs and provisioned with jdk, scala and sbt. 
