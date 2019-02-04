# Applications

@@ toc { .main depth=1 }

@@@ index
* [csw-location-server](../apps/cswlocationserver.md): Start HTTP location server application, required for running the Location Service.
* [csw-admin-server](../apps/cswadminserver.md): Start HTTP server that supports changing/getting log level of component.
* [csw-location-agent](../apps/cswlocationagent.md): Application used to start non-CSW services so that register with Location Service.
* [csw-config-server](../apps/cswonfigserverapp.md): Start a Configuration Service SVN repository.
* [csw-config-cli](../apps/cswconfigcli.md): Application to perform administrative functions on the Configuration Service.
* [csw-event-cli](../apps/csweventcli.md): Application to publish an event or subscribe to an event.
* [csw-alarm-cli](../apps/cswalarmcli.md): Application to perform administrative functions on the Alarm Service.
* [csw-host-config](../apps/hostconfig.md): Framework for creating host configuration applications, used to start multiple containers on a machine.
@@@

## Starting Apps for Development

In order to run a component it is required to run `csw-location-server`. Moreover, even to start event service or configuration service,
`csw-location-server` should be running.

But for developers to start `csw-location-server` first and then configuration service and then event service is a tedious job. So, for
development purpose, it is recommended to use a shell script which will start `csw-location-server` and/or configuration service and/or 
event service and/or alarm service.

Assuming that developer has downloaded `csw-apps-<some-version>.zip` from [csw releases](https://github.com/tmtsoftware/csw/releases)
and unzipped it.

There are three folders, as follows, in `csw-apps-<some-version>`
* bin
* lib
* and conf.

All the shell scripts provided by `csw` reside in `bin` folder. The shell script referred in this segment is named as `csw-services.sh`.
Go to the bin folder and hit `./csw-services.sh --help`. This will list all possible options applicable for the script.

@@@ note { title=Note }

This shell script will start `csw-location-server` as the first step regardless of any options provided. 

@@@

The execution of the script is such that it starts `csw-location-server`, then checks whether to start configuration service from provided options.

Possible options to start services are explained below:

`--auth` if provided, starts authentication service.
`--config` if provided, starts configuration service.
`--event` if provided, starts event service. 
`--alarm` if provided, starts alarm service.
`--database` if provided, starts database service. It is important to set `PGDATA` env var to start the postgres server.
                                                              See `--help` for more details 

@@@ note

While starting database service, make sure that
 
 * `PGDATA` environment variable is set to postgres data directory where postgres is installed e.g. for mac: "/usr/local/var/postgres" and
 * there is a password set for the valid Postgres user. If not, go to postgres shell via `psql` and run `ALTER USER <username> WITH PASSWORD '<mypassword>';`.
If there is any problem entering postgres shell, go to `conf` folder -> `database_service` -> `pg_hba.conf` and change `password` to `trust`. Try entering
postgres shell again and set the password. Once set successfully, revert `trust` to `password` in `pg_hba.conf` and run database service via `csw-services.sh`.   

@@@

With this, the component code is now ready to connect to provided services via `csw-services.sh`.   


  


 


 