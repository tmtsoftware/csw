# Applications

@@ toc { .main depth=1 }

@@@ index
* [coursier-installation](../apps/csinstallation.md): Install coursier for launching applications.
* [csw-services](../apps/cswservices.md): Start services required for running applications.
* [csw-location-server](../apps/cswlocationserver.md): Start HTTP location server application, required for running the Location Service.
* [csw-location-agent](../apps/cswlocationagent.md): Application used to start non-CSW services so that register with Location Service.
* [csw-config-server](../apps/cswonfigserverapp.md): Start a Configuration Service SVN repository.
* [csw-config-cli](../apps/cswconfigcli.md): Application to perform administrative functions on the Configuration Service.
* [csw-event-cli](../apps/csweventcli.md): Application to publish an event or subscribe to an event.
* [csw-alarm-cli](../apps/cswalarmcli.md): Application to perform administrative functions on the Alarm Service.
* [csw-host-config](../apps/hostconfig.md): Framework for creating host configuration applications, used to start multiple containers on a machine.
@@@

## Starting Elastic Logging Aggregator for Development

Elastic stack ([Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html),
           [Logstash](https://www.elastic.co/guide/en/logstash/current/index.html),
           [Kibana](https://www.elastic.co/guide/en/kibana/current/index.html) and 
           [Filebeat](https://www.elastic.co/guide/en/beats/filebeat/current/index.html)) is used to aggregate logs generated from TMT applications (Scala/Java/Python/C++/C) and
CSW services (mentioned in the previous section). For development purposes, [Docker compose](https://docs.docker.com/compose/) is used. Hence, make sure that latest
Docker setup is installed and running before starting the Elastic stack. To know more about how Elastic stack works please refer to @ref[Logging Aggregator](logging_aggregator.md).

For the host setup, follow the below given steps:

* Install [Docker](https://www.docker.com/products/container-runtime) version **18.09+**
* Install [Docker Compose](https://docs.docker.com/compose/install/) version **1.24.0+**

On distributions which have SELinux enabled out-of-the-box, you will need to either re-context the files or set SELinux
into Permissive mode in order for docker-elk to start properly. For example, on Redhat and CentOS, the following will
apply the proper context:

```console
$ chcon -R system_u:object_r:admin_home_t:s0 docker-elk/
```
To know more about running Docker for Mac please refer to this [link](https://docs.docker.com/docker-for-mac/). For Windows, ensure that the
"Shared Drives" feature is enabled for the `C:` drive (Docker for Windows > Settings > Shared Drives).
See [Configuring Docker for Windows Shared Drives](https://docs.microsoft.com/en-us/archive/blogs/stevelasker/configuring-docker-for-windows-volumes) (MSDN Blog).

Assuming that the developer has downloaded `logging-aggregator-<some-version>.zip` from [csw releases](https://github.com/tmtsoftware/csw/releases)
and unzipped it, there is one folder, as follows, in `logging-aggregator-<some-version>`:

* logging_aggregator

Go to `logging_aggreator/dev` and run

* `docker-compose build  --no-cache`
* `docker-compose up  --force-recreate`

This will start Filebeat, Elasticsearch, Logstash and Kibana in a Docker container. Note that `csw-services` application will generate all log files under 
`/tmp/tmt/logs/csw` and Filebeat will watch for them there.

Once, the Docker container is up, open an browser and go to `http://localhost:5601/` to use Kibana. Go to:

* `Management` -> `Kibana` ->  `Index Patterns` and create an index pattern as per the requirement.
* `Discover` -> `Select the index pattern created` and explore

To use a different Elastic Stack version than the one currently available in the repository, simply change the version in `logging_aggreator/dev/.env`
file, and rebuild the stack with:

* `docker-compose build  --no-cache`
* `docker-compose up  --force-recreate`

Always pay attention to the [upgrade instructions](https://www.elastic.co/guide/en/elasticsearch/reference/current/setup-upgrade.html)
for each individual component before performing a stack upgrade.


