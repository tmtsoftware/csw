# Logging Aggregator

The logging aggregation provides aggregation of logs generated from TMT applications written in Scala, Java, Python, C, C++ and modules like System logs,
Redis logs, Postgres logs, ElasticSearch logs, Keycloak logs using Elastic stack([Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html),
[Logstash](https://www.elastic.co/guide/en/logstash/current/index.html), [Kibana](https://www.elastic.co/guide/en/kibana/current/index.html))
and [Filebeat](https://www.elastic.co/guide/en/beats/filebeat/current/index.html).

## Elastic license

It is recommended to use the basic/elastic license of Elastic stack which is a free license. To know more about what features are available in basic
license refer this [link](https://www.elastic.co/subscriptions).

## Architecture

![logo](../services/logging_architecture.png)

As shown in above architecture diagram all machines in TMT can run Filebeat to watch log files. Filebeat is responsible for watching log files and shipping it
to the centralised Logstash component. Filebeat maintains a marker in a registry for the last read position in a file. Therefore, applications can keep emitting logs to the file agnostic of the Filebeat's marker.

Logstash collects the Json logs in string format, parses it to a valid Json object and then feeds it to Elasticsearch. Elasticsearch is responsible for ingesting and indexing the json data.
If logs are generated in non-Json format which will be the case for Redis, System, Postgres, Elasticsearch logs, then they will be parsed and indexed using the elastic 
[modules](https://www.elastic.co/guide/en/beats/filebeat/current/filebeat-modules-overview.html). 

After the Json data is indexed in Elasticearch, Kibana provides powerful visualisation tools and dashboards that offer various interactive diagrams to visualize complex queries. 

@@@ note

All the files required for logging aggregator configuration can be found [here](https://github.com/tmtsoftware/csw/tree/master/scripts/logging_aggregator).
How to use each of these files and it's significance will be explained further in the document.

It is assumed that in production, Elasticsearch, Logstash and Kibana will be registered with TMT infra DNS setup. Hence, all the configuration files
for production is provided referring to DNS host name. 

It is strongly recommended to run the same version (v6.6.0 or higher) of elastic components so as to avoid any compatibility issues.

By default, the elastic stack exposes the following ports and the configurations also rely on the same.
   * 5044: Logstash TCP input.
   * 9200: Elasticsearch HTTP port
   * 5601: Kibana

@@@


## TMT Applications (Scala/Java/Python/C++/C)

In order to aggregate logs generated from TMT apps, Filebeat will require to watch them. The recommended practice is for apps to generate log files
at a common place so that Filebeat can watch it. The common place to generate log files and watch them is defined by an environment variable `TMT_LOG_HOME`
For e.g. TMT_LOG_HOME = /<<user-accessible-space>>/tmt/logs.

@@@ note

Make sure that in production TMT_LOG_HOME is not set to /tmp, otherwise on machine reboot all the logs will be lost.  

@@@      

The upcoming sections will explain how each TMT app can generate log files at TMT_LOG_HOME and how Filebeat can watch from the same TMT_LOG_HOME:

### Scala/Java

For Scala/Java applications to dump logs in a file, it is important that developers enable the `FileAppender` in application.conf. To know more about how
to configure FileAppender please refer the @ref:[logging documentation](../services/logging.md#configuration). Once, the FileAppender is enabled, the log files
will be generated under `TMT_LOG_HOME`. If TMT_LOG_HOME is not set as an environment variable then `BaseLogPathNotDefined` exception will be thrown. 
For tests, `baseLogPath` from logging configuration can be overridden in application.conf.

### C++

For C++ developers it is recommended to use the [spdlog](https://github.com/gabime/spdlog) library along with 
[spdlog-setup](https://github.com/guangie88/spdlog_setup) add-on library for logging in files.

The following code snippet will explain how to use `spdlog`:

main.cpp
:   @@snip [main.cpp](../../../../examples/src/main/scala/example/logging/aggregator/cpp/main.cpp)

logging_default.toml
:   @@snip [logging_default.toml](../../../../examples/src/main/scala/example/logging/aggregator/cpp/logging_default.toml)


The source code for above code can be found [here](https://github.com/tmtsoftware/csw/tree/master/examples/src/main/scala/example/logging/aggregator/cpp)

@@@ note

Things to keep in mind while writing C++/C/Python apps

 * The structure of json logs should adhere to @ref:[this](../services/logging.md#log-structure) format
 * log files should be generated at path set at TMT_LOG_HOME
 * time should be logged in UTC
 * recommended to use rotating files for logging
 * configuration of log level should be in `.toml` file (or any config file for that matter e.g. `.conf` for C or `.json` for python) so that
   log level is changeable without re-compiling C/C++ code. 

All the above points are already covered for Scala/Java apps in logging framework.

@@@

### C

For C develoers it is recommended to use [zlog](https://github.com/HardySimpson/zlog) logging library. The following code snippet will explain how to
use `zlog`:

main.c
:   @@snip [main.c](../../../../examples/src/main/scala/example/logging/aggregator/c/main.c)

logging_default.conf
:   @@snip [logging_default.conf](../../../../examples/src/main/scala/example/logging/aggregator/c/logging_default.conf)


The source code for above code can be found [here](https://github.com/tmtsoftware/csw/tree/master/examples/src/main/scala/example/logging/aggregator/c)

@@@ note

Things to keep in mind while writing C++/C/Python apps

 * The structure of json logs should adhere to @ref:[this](../services/logging.md#log-structure) format
 * log files should be generated at path set at TMT_LOG_HOME
 * time should be logged in UTC
 * recommended to use rotating files for logging
 * configuration of log level should be in `.toml` file (or any config file for that matter e.g. `.conf` for C or `.json` for python) so that
   log level is changeable without re-compiling C/C++ code. 

All the above points are already covered for Scala/Java apps in logging framework.

@@@

### Python

For python developers it is recommended to use the default `python-logging` module that comes with python. The following code snippet will explain how to
use `python-logging` with python 3.7:

main.py
:   @@snip [main.py](../../../../examples/src/main/scala/example/logging/aggregator/python/main.py)

logging_default.json
:   @@snip [logging_default.json](../../../../examples/src/main/scala/example/logging/aggregator/python/logging_default.json)

tmt_formatter.py
:   @@snip [tmt_formatter.py](../../../../examples/src/main/scala/example/logging/aggregator/python/tmt_formatter.py)


The source code for above code can be found [here](https://github.com/tmtsoftware/csw/tree/master/examples/src/main/scala/example/logging/aggregator/python)

@@@ note

The use of `tmt_formatter.py` in `logging_default.json` to log in UTC timezone.

@@@

@@@ note

Things to keep in mind while writing C++/C/Python apps

 * The structure of json logs should adhere to @ref:[this](../services/logging.md#log-structure) format
 * log files should be generated at path set at TMT_LOG_HOME
 * time should be logged in UTC
 * recommended to use rotating files for logging
 * configuration of log level should be in `.toml` file (or any config file for that matter e.g. `.conf` for C or `.json` for python) so that
   log level is changeable without re-compiling C/C++ code. 

All the above points are already covered for Scala/Java apps in logging framework.

@@@
   
### Filebeat watching TMT app logs and system generated logs

Once TMT applications generate log files under TMT_LOG_HOME, Filebeat needs to start watching them. In order for Filebeat to be aware of TMT_LOG_HOME,
[filebeat.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/system/filebeat.yml) should be used to
start Filebeat.

All machines running TMT application also need system generated logs to be watched by Filebeat so that it gets shipped to Logstash. This can be achieved
by enabling [System module](https://www.elastic.co/guide/en/beats/filebeat/current/filebeat-module-system.html) in Filebeat and making Elasticsearch aware of receiving
system logs (text based logs) to parse and index them.

In order to achieve this follow the below given steps:

* Run Elasticsearch using [elasticsearch.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/elasticsearch/elasticsearch.yml).
  Place `elasticsearch.yml` in <<Elasticsearch installation folder>>/config/ and execute `./bin/elasticsearch` (on Mac) or `bin/elasticsearch` (on Linux).
* Run LogStash using [logstash.conf](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/logstash/logstash.conf).
  Place `logstash.conf` in <<Logstash installation folder>>/config/ and execute `./bin/logstash -f config/logstash.conf` (on mac) or
  `bin/logstash -f config/logstash.conf` (on Linux).
* Place [filebeat.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/system/filebeat.yml)
  in <<Filebeat installation folder>> and execute [filebeat-init.sh](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/system/filebeat-init.sh)
  from <<Filebeat installation folder>>. This will make Elasticsearch aware of System module.
* Run Filebeat from <<Filebeat installation folder>> by executing `./filebeat -e` (on Mac) or `filebeat -e` (on Linux).
  This will enable system module for watching system logs from its default path i.e. /var/log/system.log as well as watching log files under TMT_LOG_HOME.
* Run Kibana using [kibana.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/kibana/kibana.yml). 
  Place `kibana.yml` in <<Kibana installation folder>>/config/ and execute `./bin/kibana` (on mac) or `bin/kibana` (on Linux). This will give GUI over
  logs aggregated in Elasticsearch.

@@@ note
 
It is assumed that in production, Elasticsearch, Logstash and Kibana will be registered with TMT infra DNS setup. Hence, all the [configuration files]
(https://github.com/tmtsoftware/csw/tree/master/scripts/logging_aggregator/prod)
for production is provided referring to dns name. 
 
@@@

## Redis logs

Redis will be started in sentinel mode, master mode and slave mode on different machines for Event and Alarm service. [Configuration files]
(https://github.com/tmtsoftware/csw/tree/master/scripts/conf)
are provided for Redis sentinel, master and slave to log in file `/usr/local/var/log/redis/redis-server.log`. Filebeat will also be watching this file
once [Redis module](https://www.elastic.co/guide/en/beats/filebeat/6.7/filebeat-module-redis.html) is enabled.

Note that system generated logs on Redis machines also needs to be watched by Filebeat and aggregated. In order to enable Redis and System module
follow the below given steps: 


* Run Elasticsearch using [elasticsearch.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/elasticsearch/elasticsearch.yml).
  Place `elasticsearch.yml` in <<Elasticsearch installation folder>>/config/ and execute `./bin/elasticsearch` (on Mac) or `bin/elasticsearch` (on Linux).
* Run LogStash using [logstash.conf](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/logstash/logstash.conf).
  Place `logstash.conf` in <<Logstash installation folder>>/config/ and execute `./bin/logstash -f config/logstash.conf` (on mac) or
  `bin/logstash -f config/logstash.conf` (on Linux).
* Place [filebeat.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/redis/filebeat.yml)
  in <<Filebeat installation folder>> and execute [filebeat-init.sh](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/redis/filebeat-init.sh)
  from <<Filebeat installation folder>>. This will make Elasticsearch aware of Redis and System module.
* Run Filebeat from <<Filebeat installation folder>> by executing `./filebeat -e` (on Mac) or `filebeat -e` (on Linux).
* Run Kibana using [kibana.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/kibana/kibana.yml). 
  Place `kibana.yml` in <<Kibana installation folder>>/config/ and execute `./bin/kibana` (on mac) or `bin/kibana` (on Linux). This will give GUI over
  logs aggregated in Elasticsearch.


## Postgres logs

Logs generated by Postgres needs to be watched by Filebeat and aggregated. Hence, use [postgres.conf](https://github.com/tmtsoftware/csw/blob/master/scripts/conf/database_service/postgres.conf)
to start the PostgreSQL server which will enable logging in postgres to its default location `/usr/local/var/postgres` and use UTC time for logging.

Note that system generated logs on Postgres machine also needs to be watched by Filebeat and aggregated. In order to enable Postgres and System module
follow the below given steps: 


* Run Elasticsearch using [elasticsearch.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/elasticsearch/elasticsearch.yml).
  Place `elasticsearch.yml` in <<Elasticsearch installation folder>>/config/ and execute `./bin/elasticsearch` (on Mac) or `bin/elasticsearch` (on Linux).
* Run LogStash using [logstash.conf](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/logstash/logstash.conf).
  Place `logstash.conf` in <<Logstash installation folder>>/config/ and execute `./bin/logstash -f config/logstash.conf` (on mac) or
  `bin/logstash -f config/logstash.conf` (on Linux).
* Place [filebeat.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/postgresql/filebeat.yml)
  in <<Filebeat installation folder>> and execute [filebeat-init.sh](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/postgresql/filebeat-init.sh)
  from <<Filebeat installation folder>>. This will make Elasticsearch aware of Postgres and System module.
* Run Filebeat from <<Filebeat installation folder>> by executing `./filebeat -e` (on Mac) or `filebeat -e` (on Linux).
* Run Kibana using [kibana.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/kibana/kibana.yml). 
  Place `kibana.yml` in <<Kibana installation folder>>/config/ and execute `./bin/kibana` (on mac) or `bin/kibana` (on Linux). This will give GUI over
  logs aggregated in Elasticsearch.

## Elasticsearch logs

It is important to also aggregate logs generated by Elasticsearch. There can be situations where indexing generates error and it will be useful to have
those errors aggregated and viewed in Kibana. Hence, use [elaticsearch.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/elasticsearch/elasticsearch.yml)
to start Elasticsearch which will configure log file location under `TMT_LOG_HOME`.

Note that system generated logs on Elasticsearch machine also needs to be watched by Filebeat and aggregated. In order to enable Elasticsearch and System
module follow the below given steps:

* Run Elasticsearch using [elasticsearch.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/elasticsearch/elasticsearch.yml).
  Place `elasticsearch.yml` in <<Elasticsearch installation folder>>/config/ and execute `./bin/elasticsearch` (on Mac) or `bin/elasticsearch` (on Linux).
* Run LogStash using [logstash.conf](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/logstash/logstash.conf).
  Place `logstash.conf` in <<Logstash installation folder>>/config/ and execute `./bin/logstash -f config/logstash.conf` (on mac) or
  `bin/logstash -f config/logstash.conf` (on Linux).
* Place [filebeat.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/elasticsearch/filebeat.yml)
  in <<Filebeat installation folder>> and execute [filebeat-init.sh](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/filebeat/modules/elasticsearch/filebeat-init.sh)
  from <<Filebeat installation folder>>. This will make Elasticsearch aware of it's own module and System module.
* Run Filebeat from <<Filebeat installation folder>> by executing `./filebeat -e` (on Mac) or `filebeat -e` (on Linux).
  This will enable elasticsearch and system module.
* Run Kibana using [kibana.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/kibana/kibana.yml). 
  Place `kibana.yml` in <<Kibana installation folder>>/config/ and execute `./bin/kibana` (on mac) or `bin/kibana` (on Linux). This will give GUI over
  logs aggregated in Elasticsearch.

## Keycloak logs

Logs generated from Keycloak needs to be watched by Filebeat and aggregated. Hence, use [standalone.xml](https://github.com/tmtsoftware/csw/blob/master/scripts/conf/auth_service/standalone.xml)
to start JBoss server which will make JBoss server to  log in json format, enable keycloak logs and generate log files under `TMT_LOG_HOME`.

Note that system generated logs on Keycloak machine also needs to be watched by Filebeat and aggregated. In order to watch Keycloak logs from 
`TMT_LOG_HOME` and enable system module refer the steps from @ref:[here](logging_aggregator.md#filebeat-watching-tmt-app-logs-and-system-generated-logs).  

## System logs on Logstash and Kibana machines

Machines running Logstash and Kibana will require to aggregate system logs from their machines too. Hence, in order to enable system module on their
machines follow the below given steps:

* Run Elasticsearch using [elasticsearch.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/elasticsearch/elasticsearch.yml).
  Place `elasticsearch.yml` in <<Elasticsearch installation folder>>/config/ and execute `./bin/elasticsearch` (on Mac) or `bin/elasticsearch` (on Linux).
* Go to <<Filebeat installation folder>> place [Logstash/filebeat.yml](https://github.com/tmtsoftware/csw/tree/master/scripts/logging_aggregator/prod/logstash/filebeat.yml) 
  and execute [Logstash/filebeat-init.sh](https://github.com/tmtsoftware/csw/tree/master/scripts/logging_aggregator/prod/logstash/filebeat-init.sh) or
  place [Kibana/filebeat.yml](https://github.com/tmtsoftware/csw/tree/master/scripts/logging_aggregator/prod/kibana/filebeat.yml)
  and execute [Kibana/filebeat-init.sh](https://github.com/tmtsoftware/csw/tree/master/scripts/logging_aggregator/prod/kibana/filebeat-init.sh) 
  from Logstash or Kibana machines respectively. This will make Elasticsearch aware of System module.
* Run Filebeat from <<Filebeat installation folder>> by executing `./filebeat -e` (on Mac) or `filebeat -e` (on Linux).
* Run LogStash using [logstash.conf](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/logstash/logstash.conf).
  Place `logstash.conf` in <<Logstash installation folder>>/config/ and execute `./bin/logstash -f config/logstash.conf` (on mac) or
  `bin/logstash -f config/logstash.conf` (on Linux).
* Run Kibana using [kibana.yml](https://github.com/tmtsoftware/csw/blob/master/scripts/logging_aggregator/prod/kibana/kibana.yml). 
  Place `kibana.yml` in <<Kibana installation folder>>/config/ and execute `./bin/kibana` (on mac) or `bin/kibana` (on Linux). This will give GUI over
  logs aggregated in Elasticsearch.

## Explore Kibana

Once Kibana is up and running hit `http://localhost:5601/` in browser and go to:
                              
* `Management` -> `Kibana` ->  `Index Patterns` and create an index pattern as per the requirement.
* `Discover` -> `Select the index pattern created` and explore

For Modules like System, Postgres, Redis and Elasticsearch go to `Dashboard` and explore.

## Running ELK for developer

For development purposes [Docker compose](https://docs.docker.com/compose/) is used to start Elasticsearch, Logstash, Kibana and Filebeat in a container.
Hence, make sure that latest Docker setup is installed and running before starting the ELK stack. Docker container is responsible to aggregate logs
generated in `tmp/csw/logs`. Hence, developers writing Scala/Java/Python/C++/C applications need to generate log files under `/tmp/csw/logs`. 

Also, note that csw apps started via `csw-services.sh` will generate log files under `/tmp/csw/logs` and thus, it will be aggregated by the ELK docker container.
To know more about setting up docker and starting ELK please refer @ref:[Starting ELK logging aggregator for Development](apps.md#starting-elk-logging-aggregator-for-development). 