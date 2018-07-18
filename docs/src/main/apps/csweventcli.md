# csw-event-cli

A command line application that facilitates interaction with Event Service. It accepts various commands to publish and subscribe to events.

## Supported Commands

* inspect
* get
* publish
* subscribe

### inspect

Takes an comma separated list of events and displays event's parameter information which includes key name, key type and unit along with metadata (event key, timestamp & id).

 * `-e`, `--events` : comma separated list of events to inspect
 
#### Examples:

```
csw-event-cli inspect -e wfos.prog.cloudcover,wfos.prog.filter
```

@@@ note
`inspect` command does not display parameter values. To view values, use `get` command instead.
@@@
 
### get

Takes an comma separated list of events with nested key paths and displays event information including values either in oneline or json format.

 * `-e`, `--events`     comma separated list of events in the form of `<event1:key1>,<event2:key2:key3>`, use `:` to separate multiple keys for same event. Ex. `-e a.b.c:struct1/ra,x.y.z:struct2/dec:epoch`
 * `-o`, `--out`        output format, default is oneline
 * `-t`, `--timestamp`  display timestamp
 * `--id`               display event id
 * `-u`, `--units`      display units

#### Examples:

1. 
```
csw-event-cli get -e wfos.prog.cloudcover
```
Displays all keys information in oneline form for event `wfos.prog.cloudcover`

2. 
```
csw-event-cli get -e wfos.prog.cloudcover:struct1/ra:epoch -t --id -u
```
Displays information of only `struct1/ra` and `epoch` keys as well as `timestamp`, `event id` and `units` of provided keys in oneline form for event `wfos.prog.cloudcover`

3. 
```
csw-event-cli get -e wfos.prog.cloudcover:epoch,wfos.prog.filter:ra
```
Displays information of `epoch` of event `wfos.prog.cloudcover` and `ra` key of event `wfos.prog.filter:ra`

4. 
```
csw-event-cli get -e wfos.prog.cloudcover:epoch -o json
```
Displays event `wfos.prog.cloudcover` with only `epcoh` key in json format.

@@@ note
`-t`, `--id` & `--u` options are not applicable when `-o json` option is provided. Event displayed in `json` format will always have `timestamp`, `event id` and `units` irrespective of whether those options are provided or not via cli.
@@@

 
### publish

Publishes event to event server from provided input data file or cli params.

 * `-e`, `--event`      event key to publish
 * `--data`             absolute file path which contains event in json format
 * `--params`           pipe '|' separated list of params enclosed in double quotes in the form of `"keyName:keyType:unit=values| ..."`. unit is optional here. Supported key types are: 
                        `[i = IntKey | s = StringKey | f = FloatKey | d = DoubleKey | l = LongKey | b = BooleanKey]`.
                        You can optionally choose to enclose param values in \[, \] brackets.
                        Values of string key should be provided in single quotes and use '\' to escape string.
                        Ex. `"addressKey:s=['Kevin O\'Brien','Chicago, USA']|timestampKey:s=['2016-08-05T16:23:19.002']"`
 * `-i`, `--interval`   interval in `<ms>` to publish event, single event will be published if not provided
 * `-p`, `--period`     publish events for this duration `<seconds>` on provided interval. Default is `2147483` seconds.

@@@ note
If `--data` & `--params` are provided together, then event is generated from both `--data` file & `--params` option.
`--params` takes a precedence and overrides params from event data file if it already presents in the file.

Option `-p` should be used with `-i`, otherwise `-p` is ignored. 
@@@

#### Examples:

1. 
```
csw-event-cli publish -e wfos.prog.cloudcover --data /path/to/event.json
```
Creates event from provided json file and publishes same with key `wfos.prog.cloudcover` to event server. 

2. 
```
csw-event-cli publish -e wfos.prog.cloudcover --data /path/to/event.json -i 500 -p 60
```
Creates event from provided json file and publishes same event at every `500ms` for duration of `60s`. 

3. 
```
csw-event-cli publish -e wfos.prog.cloudcover --params "k1:s=['Kevin O\'Brien','Chicago, USA']|k2:s=['2016-08-05T16:23:19.002']"
```
First fetches already published event for key `wfos.prog.cloudcover` from event server and then update's that event with provided `--params`
If provided keys already presents in existing event, then those will be updated else new param entries will be added to event.
If no event is published in past for provided key, then new event gets created with provided params and event key. 

### subscribe

Takes an comma separated list of events with nested key paths and displays continuous stream of event information as soon as it receives event. 

 * `-e`, `--events`     comma separated list of events in the form of `<event1:key1>,<event2:key2:key3>`, use `:` to separate multiple keys for same event. Ex. `-e a.b.c:struct1/ra,x.y.z:struct2/dec:epoch`
 * `-i`, `--interval`   interval in `<ms>`, receive an event exactly at each interval
 * `-o`, `--out`        output format, default is oneline
 * `-t`, `--timestamp`  display timestamp
 * `--id`               display event id
 * `-u`, `--units`      display units

#### Examples:

1. 
```
csw-event-cli subscribe -e wfos.prog.cloudcover
```
Subscribes to event key `wfos.prog.cloudcover` and displays all keys information as soon as their is a event published for key `wfos.prog.cloudcover` in the form of oneline.

2. 
```
csw-event-cli subscribe -e wfos.prog.cloudcover:struct1/ra:epoch -t --id -u
```
Subscribes to event key `wfos.prog.cloudcover` and displays information of only `struct1/ra` and `epoch` keys 
along with `timestamp`, `event id` and `units` of provided keys in oneline form as soon as their is a event published for key `wfos.prog.cloudcover`.

3. 
```
csw-event-cli subscribe -e wfos.prog.cloudcover -i 500
```
Subscribes to event key `wfos.prog.cloudcover` and displays all keys information at provided interval <500ms>.
Irrespective of whether their is multiple event's published for key `wfos.prog.cloudcover` within `500ms` interval or no event published, 
at every tick i.e. 500ms, latest event information will be displayed on the console. 

4. 
```
csw-event-cli subscribe -e wfos.prog.cloudcover:epoch -o json
```
Subscribes to event key `wfos.prog.cloudcover` and displays only `epoch` key information as soon as their is a event published for key `wfos.prog.cloudcover` in the form of json.

@@@ note
`-t`, `--id` & `--u` options are not applicable when `-o json` option is provided. Event displayed in `json` format will always have `timestamp`, `event id` and `units` irrespective of whether those options are provided or not via cli.
@@@

## About this application 
 
### `--help` 
Prints the help message.

### `--version` 
Prints the version of the application.

## Testing/Development
While testing or development, in order to use this cli application, below prerequisites must be satisfied:  

*  @ref:[csw-cluster-seed](./../apps/cswclusterseed.md) application is running.
*  @ref:[csw-location-agent](./../apps/cswlocationagent.md) application is running, which has started event server and registered it to location service.

Please refer @ref:[Starting apps for development](./../commons/apps.md#starting-apps-for-development) section for more details on how to start these applications using `csw-services.sh` script.

## Monitor statistic

`Event Service` uses [redis](https://redis.io/) as a event store. Using `redis-cli`, you can monitor continuous stats about Event service.

```
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

In above example, new line is printed every second with useful information and the difference between the old data point. 

* `keys`: Represents Event Keys
* `clients`: Represents total number of clients currently connected to redis server
* `requests`: Represents total number of redis commands processed along with delta between every interval, in this case 1 second
* `connections`: Represents total number of connections opened by redis server

The -i <interval> option in this case works as a modifier in order to change the frequency at which new lines are emitted. The default is one second.

You can explicitly pass hostname and port of redis server while running `redis-cli`
```
$ redis-cli -h redis.tmt.org -p 6379
```

Detailed list of operations you can perform with `redis-cli` can be found [here](https://redis.io/topics/rediscli) 