# Location Http Service

Once the Location server is up and running, it can be used to access the following:

## Register a connection

Register
:   @@snip [location.http](../../../../tools/http/location.http) { #register }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #register-response }

### Re-register same connection

Re-register same connection
:   @@snip [location.http](../../../../tools/http/location.http) { #re-register }

500 Internal server error
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #other-location-registered-response }

### Malformed Request

Malformed Request
:   @@snip [location.http](../../../../tools/http/location.http) { #invalid-register }

400 Bad Request
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #bad-request-response }

## Unregister a connection

Unregister
:   @@snip [location.http](../../../../tools/http/location.http) { #un-register }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #un-register-response }

### Malformed Request 

Malformed Request
:   @@snip [location.http](../../../../tools/http/location.http) { #invalid-un-register }

400 Bad Request
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #un-register-bad-request-response }

## Find a location

Find
:   @@snip [location.http](../../../../tools/http/location.http) { #find }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #find-response }

* 400 Bad Request 

## Resolve a location

Resolve
:   @@snip [location.http](../../../../tools/http/location.http) { #resolve }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #resolve-response }

* 400 Bad Request 

## List all locations

List
:   @@snip [location.http](../../../../tools/http/location.http) { #list }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #list-response }

* 400 Bad Request

## List all locations by component type

List by component type
:   @@snip [location.http](../../../../tools/http/location.http) { #list-by-componenttype }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #list-by-componenttype-response }

* 400 Bad Request

## List all locations by hostname

List by hostname
:   @@snip [location.http](../../../../tools/http/location.http) { #list-by-hostname }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #list-by-hostname-response }

* 400 Bad Request

## List all locations by connection type

List by connection type
:   @@snip [location.http](../../../../tools/http/location.http) { #list-by-connectiontype }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #list-by-connectiontype-response }

* 400 Bad Request

## List all locations by prefix

List by prefix
:   @@snip [location.http](../../../../tools/http/location.http) { #list-by-prefix }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #list-by-prefix-response }

* 400 Bad Request

## Unregister all connections

Unregister all
:   @@snip [location.http](../../../../tools/http/location.http) { #un-register-all }

200 Ok
:   @@snip [location-responses.http](../../../../tools/http/location-responses.http) { #unregister-all-response }

* 400 Bad Request

## Track a connection

Track (Websocket) 

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

* @github[location.http](/tools/http/location.http)

@@@ note

The `location.http` has first class support to execute http requests directly from IDEs like `IntelliJ IDEA` provided location server
is already running.

@@@