# csw-services

`csw-services` is an application that allows to start all the services offered by CSW.
It by default starts all the services when no option is provided. Starting specific services are possible by passing arguments corresponding to specific
services.

@@@ note

This page assumes that you have already installed and setup @ref:[coursier](csinstallation.md) { open=new }

@@@

## Install csw-services app

Following command creates an executable file named `csw-services` in the default installation directory.

```bash
cs install csw-services:<version | SHA>
```

@@@ note

If you don't provide the version or SHA in above command, `csw-services` will be installed with the latest tagged binary of `csw-services`

@@@

## Supported Commands

@@@ warning  { title='Caution' }

`csw-services` only requires the following things to be set before starting the Database Service:

- The `PGDATA` environment variable set to the Postgres data directory where Postgres is installed e.g. for mac: "/usr/local/var/postgres".
- Password set for the valid Postgres user. If not, go to the Postgres shell via `psql` and run `ALTER USER <username> WITH PASSWORD '<mypassword>';`.

Also it is not required to set `INTERFACE_NAME` environment variable explicitly but if in any case it is not assigned correctly, For Example,
if multiple interfaces are present on the machine, then one needs to set it explicitly.

@@@

### start

This command starts all the CSW services when no additional options provided.

```bash
csw-services start
```

`start` command support following additional options:

- --inside-interface-name | -i if provided, helps you set the inside interface name.

    ```bash
    // This starts all the services and sets the inside interface name to en0.
    csw-services start -i en0
    ```  
- --outside-interface-name | -o if provided, helps you set the outside interface name.

    ```bash
    // This starts all the services and sets the outside interface name to en0.
    csw-services start -o en0
    ```  
  
If only  specific services are to be started then following options can be used along with the `start` command.
  
- --config | -c if provided, starts configuration service.

    ```bash
    // This starts location service along with the config service
    csw-services start -c
    ```

- --event | -e if provided, starts event service.

    ```bash
    // This starts location service along with the event service
    csw-services start -e
    ```

- --alarm | -a if provided, starts alarm service.

    ```bash
    // This starts location service along with the alarm service
    csw-services start -a
    ```
  
- --database | -d if provided, starts database service.

    ```bash
    // This starts location service along with the database service
    csw-services start -d
    ```
  
- --auth | -k if provided, starts authentication service.

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

## Running latest master of csw-services on developer machine

 To run the latest master on dev machine, the command `sbt run` can be used.

```bash
// The below command starts all the services offered by CSW
sbt "csw-services/run start"
```
