TMT Common Software (CSW)
=========================
[![Build Status](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-dev/badge/icon)](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/csw-prod-dev/)

Common Software is the package of services and infrastructure software that integrates the [TMT](http://www.tmt.org) software systems.

See [here](https://tmtsoftware.github.io/csw-prod/) for a detailed description of the CSW software.


Version History
---------------

| Date | Tag | Docs | Source | Download |
|-----|-----|--------|-----|-----|
| 2018-04-04 | 0.4.0-RC1 | [scala](https://tmtsoftware.github.io/csw-prod/0.4.0-RC1/api/scala/index.html), [java](https://tmtsoftware.github.io/csw-prod/0.4.0-RC1/api/java/index.html) | [csw-0.4.0-RC1](https://github.com/tmtsoftware/csw-prod/tree/v0.4.0-RC1)| Source: [tar.gz](https://github.com/tmtsoftware/csw-prod/archive/v0.4.0-RC1.tar.gz), [zip](https://github.com/tmtsoftware/csw-prod/archive/v0.4.0-RC1.zip) |
| 2016-12-03 | v0.3-PDR | [API](http://tmtsoftware.github.io/csw/) | [csw-0.3](https://github.com/tmtsoftware/csw/tree/v0.3-PDR)| Source: [tar.gz](https://github.com/tmtsoftware/csw/archive/v0.3-PDR.tar.gz), [zip](https://github.com/tmtsoftware/csw/archive/v0.3-PDR.zip), Scala API Docs: [tar.gz](https://github.com/tmtsoftware/csw/releases/download/v0.3-PDR/csw-0.3-scaladoc.tar.gz), Java API Docs: [tar.gz](https://github.com/tmtsoftware/csw/releases/download/v0.3-PDR/csw-0.3-javadoc.tar.gz) |
| 2015-11-18 | v0.2-PDR | [API](https://cdn.rawgit.com/tmtsoftware/csw/CSW-API-0.2-PDR/index.html) | [csw-0.2](https://github.com/tmtsoftware/csw/tree/v0.2-PDR)| Source: [tar.gz](https://github.com/tmtsoftware/csw/archive/v0.2-PDR.tar.gz), [zip](https://github.com/tmtsoftware/csw/archive/v0.2-PDR.zip), API Docs: [tar.gz](https://github.com/tmtsoftware/csw/archive/CSW-API-0.2-PDR.tar.gz), [zip](https://github.com/tmtsoftware/csw/archive/CSW-API-0.2-PDR.zip) |


Example code can be found in the [documentation](https://tmtsoftware.github.io/csw-prod/) and in the [examples](examples) subproject.

You can find the Scala API documentation for CSW [here](https://tmtsoftware.github.io/csw-prod/api/scala/csw/index.html).

The Java API docs are [here](https://tmtsoftware.github.io/csw-prod/api/java/?/index.html).

Build Instructions
------------------

To build everything, including the documentation, run:

    sbt publishLocal stage makeSite

See [here](https://tmtsoftware.github.io/csw-prod/commons/sbt-tasks.html) for a description of the sbt tasks.

