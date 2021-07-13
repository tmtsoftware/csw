# csw-event-cli

A command line application that facilitates interaction with @ref:[Event Service](./../services/event.md). It accepts various commands to publish and subscribe to events.

## Prerequisite

- Location Service should be running
- Event Service should be running

@@@ note

This page assumes that you have already installed and setup @ref:[coursier](csinstallation.md) { open=new }

@@@

## Install csw-event-cli app

Following command creates an executable file named csw-event-cli in the default installation directory.

```bash
cs install csw-event-cli:<version | SHA>
```

Note: If you don't provide the version or SHA in above command, `csw-event-cli` will be installed with the latest tagged binary of `csw-event-cli`

## Supported Commands

- inspect
- get
- publish
- subscribe

### inspect

Takes a comma separated list of events and displays each event's parameter information which includes key name, key type, and unit along with metadata (event key, timestamp & id).

- `-e`, `--events` : comma separated list of events to inspect

#### Example

```bash
csw-event-cli inspect -e wfos.prog.cloudcover,wfos.prog.filter
```

@@@ note
`inspect` command does not display parameter values. To view values, use `get` command instead.
@@@

### get

Takes a comma separated list of events with nested key paths and displays event information including values either in one-line or JSON format.

- `-e`, `--events`     comma separated list of events in the form of `<event1:key1>,<event2:key2:key3>`, use `:` to separate multiple keys for same event. Ex. `-e a.b.c:struct1/ra,x.y.z:struct2/dec:epoch`
- `-o`, `--out`        output format, default is one-line
- `-t`, `--timestamp`  display timestamp
- `--id`               display event id
- `-u`, `--units`      display units

#### Examples

1. Displays all keys information in one-line form for event `wfos.prog.cloudcover`

    ```bash
    csw-event-cli get -e wfos.prog.cloudcover
    ```

2. Displays information of only `struct1/ra` and `epoch` keys as well as `timestamp`, `event id` and `units` of provided keys in one-line form for event `wfos.prog.cloudcover`

    ```bash
    csw-event-cli get -e wfos.prog.cloudcover:struct1/ra:epoch -t --id -u
    ```

3. Displays information of `epoch` of event `wfos.prog.cloudcover` and `ra` key of event `wfos.prog.filter:ra`

    ```bash
    csw-event-cli get -e wfos.prog.cloudcover:epoch,wfos.prog.filter:ra
    ```

4. Displays event `wfos.prog.cloudcover` with only `epcoh` key in JSON format.

     ```bash
    csw-event-cli get -e wfos.prog.cloudcover:epoch -o json
    ```

@@@ note
`-t`, `--id` & `--u` options are not applicable when `-o json` option provided. An Event displayed in JSON format will always have `timestamp`, `event id` and `units` irrespective of whether those options are provided via the CLI.
@@@

### publish

Publishes an event to the Event Server from the provided input data file or CLI params.

- `-e`, `--event`      event key to publish
- `--data`             absolute file path which contains event in JSON format
- `--params`           pipe '|' separated list of params enclosed in double quotes in the form of `"keyName:keyType:unit=values| ..."`. unit is optional here. Supported key types are: 
                        `[i = IntKey | s = StringKey | f = FloatKey | d = DoubleKey | l = LongKey | b = BooleanKey]`.
                        You can optionally choose to enclose param values in \[, \] brackets.
                        Values of a string key should be provided in single quotes and use backslash to escape string.
                        Ex. `"addressKey:s=['Kevin O\'Brien','Chicago, USA']|timestampKey:s=['2016-08-05T16:23:19.002']"`
- `-i`, `--interval`   interval in milliseconds to publish event. A single event will be published, if not provided
- `-p`, `--period`     publish events for this duration in seconds on provided interval. Default is `2147483` seconds.

@@@ note
If `--data` & `--params` are provided together, then the Event is generated from both `--data` file & `--params` option.
`--params` takes a precedence and overrides params from the Event data file if it is already present in the file.

Option `-p` should be used with `-i`, otherwise `-p` is ignored. 
@@@

#### Examples

1. Creates event from provided JSON file and publishes it with key `wfos.prog.cloudcover` to the Event Server.

    ```bash
    csw-event-cli publish -e wfos.prog.cloudcover --data /path/to/event.json
    ```

2. Creates an Event from provided JSON file and publishes it every `500ms` for duration of `60s`.

    ```bash
    csw-event-cli publish -e wfos.prog.cloudcover --data /path/to/event.json -i 500 -p 60
    ```

3. First fetches already published Event for key `wfos.prog.cloudcover` from the Event Server and then updates that Event with provided `--params`
If provided keys are already present in existing Event, then those will be updated.  Otherwise, new param entries will be added to the Event.
If no Event is published in past for the provided key, then the new Event gets created with the provided params and Event key.

    ```bash
    csw-event-cli publish -e wfos.prog.cloudcover --params "k1:s=['Kevin O\'Brien','Chicago, USA']|k2:s=['2016-08-05T16:23:19.002']"
    ```

### subscribe

Takes a comma separated list of Events with nested key paths and displays continuous stream of Event information as soon as it receives the Event. 

- `-e`, `--events`     comma separated list of Events in the form of `<event1:key1>,<event2:key2:key3>`, use `:` to separate multiple keys for the same Event. Ex. `-e a.b.c:struct1/ra,x.y.z:struct2/dec:epoch`
- `-i`, `--interval`   interval in milliseconds which to receive an Event
- `-o`, `--out`        output format, default is one-line
- `-t`, `--timestamp`  display timestamp
- `--id`               display event id
- `-u`, `--units`      display units

#### Examples

1. Subscribes to Event key `wfos.prog.cloudcover` and displays all key information as soon as there is an Event published for key `wfos.prog.cloudcover` with the one-line format.

    ```bash
    csw-event-cli subscribe -e wfos.prog.cloudcover
    ```

2. Subscribes to the Event key `wfos.prog.cloudcover` and displays information of only the `struct1/ra` and `epoch` keys 
along with `timestamp`, `event id` and `units` of tge provided keys in one-line format as soon as there is an Event published for the key `wfos.prog.cloudcover`.

    ```bash
    csw-event-cli subscribe -e wfos.prog.cloudcover:struct1/ra:epoch -t --id -u
    ```

3. Subscribes to the Event key `wfos.prog.cloudcover` and displays all key information at provided interval <500ms>.
Irrespective of whether there are multiple Events published for the key `wfos.prog.cloudcover` within `500ms` interval or not, 
at every tick (i.e. 500ms), the latest Event information will be displayed on the console. 

    ```bash
    csw-event-cli subscribe -e wfos.prog.cloudcover -i 500
    ```

4. Subscribes to Event key `wfos.prog.cloudcover` and displays only `epoch` key information as soon as there is an Event published for key `wfos.prog.cloudcover`, in JSON format.

    ```bash
    csw-event-cli subscribe -e wfos.prog.cloudcover:epoch -o json
    ```

@@@ note
`-t`, `--id` & `--u` options are not applicable when the `-o json` option is provided. An Event displayed in `json` format will always have `timestamp`, `event id` and `units` irrespective of whether those options are provided via the CLI.
@@@

## About this application

Prints the help message.

```bash
csw-event-cli --help
```

Prints the version of the application.

```bash
csw-event-cli --version
```

@@@ note

All the above examples require that `location-server` is running on local machine at `localhost:7654`.
If `location-server` is running on a remote machine with an IP address of `172.1.1.2`, then you need to pass the additional `--locationHost 172.1.1.2` command line argument.
Example:
`.\csw-event-cli get -e wfos.prog.cloudcover --locationHost 172.1.1.2`

@@@

## Testing/Development

While testing or development, in order to use this CLI application, below prerequisites must be satisfied:  

- Location Service is running.
- Event Service is running and is registered to the Location Service.

Please refer to @ref:[Starting Apps for Development](./../apps/cswservices.md) section for more details on how to start these applications using `csw-services`.

## Monitor statistics

`Event Service` uses [redis](https://redis.io/) as the event store. Using `redis-cli`, you can monitor continuous stats about the Event Service.

```bash
$ redis-cli --stat
------- data ------ --------------------- load -------------------- - child -
keys       mem      clients blocked requests            connections
305        20.70M   605     0       1771418 (+0)        615
305        20.71M   605     0       1825363 (+53945)    615
305        20.70M   605     0       1877638 (+52275)    615
305        20.71M   605     0       1910198 (+32560)    615
305        20.71M   605     0       1960837 (+50639)    615
305        20.74M   605     0       2001565 (+40728)    615
```

In the above example, a new line is printed every second with useful information, including the difference between current and old data points. 

- `keys`: Represents all the keys present in the Redis database, which in case of the Event Service are EventKeys
- `clients`: Represents total number of clients currently connected to the Redis server
- `requests`: Represents total number of Redis commands processed along with a delta between every interval, specified with the `-i` option (see below)
- `connections`: Represents total number of socket connections opened to the Redis server

The `-i <interval>` option in this case works as a modifier in order to change the frequency at which new lines are emitted. The default is one second.

You can explicitly pass the hostname and port of the Redis server while running `redis-cli`

```bash
$ redis-cli -h redis.tmt.org -p 6379
```

A detailed list of operations you can perform with `redis-cli` can be found [here](https://redis.io/topics/rediscli)
