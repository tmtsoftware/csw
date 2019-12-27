# Location Http Service

Once the Location server is up and running, it can be used to access the following:

## Register a connection

Http
:   @@snip [location.http](../../../../tools/location.http) { #register }

## Unregister a connection

Http
:   @@snip [location.http](../../../../tools/location.http) { #un-register }

## Find a location

Http
:   @@snip [location.http](../../../../tools/location.http) { #find }

## Resolve a location

Http
:   @@snip [location.http](../../../../tools/location.http) { #resolve }

## List all locations

Http
:   @@snip [location.http](../../../../tools/location.http) { #list }

## List all locations by component type

Http
:   @@snip [location.http](../../../../tools/location.http) { #list-by-componenttype }

## List all locations by hostname

Http
:   @@snip [location.http](../../../../tools/location.http) { #list-by-hostname }

## List all locations by connection type

Http
:   @@snip [location.http](../../../../tools/location.http) { #list-by-connectiontype }

## List all locations by prefix

Http
:   @@snip [location.http](../../../../tools/location.http) { #list-by-prefix }

## Unregister all connections

Http
:   @@snip [location.http](../../../../tools/location.http) { #un-register-all }

## Track a connection

Websocket

```.http request
ws://localhost:7654/websocket-endpoint

{
  "Track": {
    "prefix": "csw.server",
    "componentType": "service",
    "connectionType": "http"
  }
}

```

@@@ note

One can use websocket plugin for [IntelliJ](https://plugins.jetbrains.com/plugin/7980-websocket-client/) or [chrome extension]
(https://chrome.google.com/webstore/detail/simple-websocket-client/pfdhoblngboilpfeibdedpjgfnlcodoo?hl=en) to test the track functionality
of location service.

@@@  

## Source code for examples

* @github[location.http](/tools/location.http)

@@@ note

The `location.http` has first class support to execute http requests directly from IDEs like `IntelliJ IDEA` provided location server
is already running.

@@@