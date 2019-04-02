# Alarm Service Technical Description

The Alarm Service implementation is based on [Redis](https://redis.io/).
Setting an alarm's severity involves setting a value in Redis that expires after a certain time, if not refreshed. 
A [keyspace notification](https://redis.io/topics/notifications)
is received from Redis when an alarm's severity changes or expires. 
An expired alarm severity is interpreted to be *Disconnected*.

@@@note 

See the section on @ref:[using alarms](../../commons/using-alarms.md) as well as the @ref:[Alarm Reference Manual](../../apps/cswalarmcli.md) for more details on using the Alarm Service in CSW applications.

@@@

