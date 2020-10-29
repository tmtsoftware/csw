# csw-services

@@@ note

This page assumes that you have already installed and set-up coursier : @ref:[coursier-installation](csinstallation.md) { open=new }.

@@@

## Starting Apps for Development

1.  In order to run a component, it is required to run @ref:[csw-location-server](cswlocationserver.md).
Moreover, even to start applications like @ref:[csw-event-cli](csweventcli.md) and @ref:[csw-config-cli](cswconfigcli.md)
along with Location Service, services like Event Service and Config Service are required to be up and running respectively.
 
2. csw-services is an application that has been provided to make the starting of services more convenient for developers.
   This application can be used to start the Location Service, Event Service or any set of services that are required by 
   the developer.
   
### Install csw-services app

Following command creates an executable file named csw-services in the default installation directory.

```bash
cs install csw-services:<version | SHA>
```

@@@ note

If you don't provide the version or SHA in above command, `csw-services` will be installed with the latest tagged binary of `csw-services`

@@@ 

### Supported Commands

The below command starts all the CSW services.

* start
```bash
csw-services start
```


If only  specific services are to be started then following options can be used along with the `start` command.

* --config | -c if provided, starts configuration service.
    ```bash
    // This starts location service along with the config service
    csw-services start -c
    ```
* --event | -e if provided, starts event service.
    ```bash
    // This starts location service along with the event service
    csw-services start -e
    ```
* --alarm | -a if provided, starts alarm service.
    ```bash
    // This starts location service along with the alarm service
    csw-services start -a
    ```
* --database | -d if provided, starts database service.
    ```bash
    // This starts location service along with the database service
    csw-services start -d
    ```

@@@ note

While starting the Database Service i.e running the command `csw-services start -d`, make sure that
 
* The `PGDATA` environment variable is set to the Postgres data directory where Postgres is installed e.g. for mac: "/usr/local/var/postgres" and
* there is a password set for the valid Postgres user. If not, go to the Postgres shell via `psql` and run `ALTER USER <username> WITH PASSWORD '<mypassword>';`.

@@@
  
* --auth | -k if provided, starts authentication service.
    ```bash
    // This starts location service along with the auth service
    csw-services start -k
    ```
If multiple services are to be started, multiple options can be given at once
```bash
// This starts location service along with the auth service and event service
csw-services start -k -e
```

@@@ note

By default `csw-services` application runs services in the foreground, you can press `ctr+c` to stop all the services.

@@@

    