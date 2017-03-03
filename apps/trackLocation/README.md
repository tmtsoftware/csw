Track Location
==============

This project provides a standalone application that is a simple wrapper for an external application that
registers it with the location service and unregisters it when it exits.

```
trackLocation 0.2-SNAPSHOT
Usage: trackLocation [options] [<app-config>]

  --name <name>
        Required: The name used to register the application (also root name in config file)
  --cs-name <value>
        optional name of the config service to use (for fetching the application config file)
  -c <name> | --command <name>
        The command that starts the target application: use %port to insert the port number
        (default: use $name.command from config file: Required)
  -p <number> | --port <number>
        Optional port number the application listens on (default: use value of $name.port
        from config file, or use a random, free port.)
  <app-config>
        optional config file in HOCON format (Options specified as:
        $name.command, $name.port, etc. Fetched from config service if path does not exist)
  --no-exit
        for testing: prevents application from exiting after running command
  --help

  --version
 ```

Example Usage
-------------

One way to start and track a Redis instance is to use this command:

    tracklocation --name redisTest --command 'redis-server --port 7777' --port 7777

Or you can put the settings in a config file: redisTest.conf

```
redisTest {
  port = 7777
  command = redis-server --port 7777
}
```

And then run this command:

    tracklocation --name redisTest redisTest.conf

If the config file is stored in the Config Service under test/redisTest.conf, you can use this command:

    tracklocation --name redisTest test/redisTest.conf

If the path name is not found locally, it is searched for with the config service.
You can specify the name of the config service to use (if other than the default) with the --cs-name option:

    tracklocation --name redisTest --cs-name myConfigService test/redisTest.conf

This assumes that a config service application is running and is registered with the location
service under the name `myConfigService`.