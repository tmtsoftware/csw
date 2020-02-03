# Service contracts

This page describes possible ways of accessing various services via HTTP/Websocket and sample JSON representation of request body, response body and error body. 

### Http endpoint

The url - `http://{{host}}:{{port}}/post-endpoint` can be used to access running service(e.g. location-service, command-service)

### Websocket endpoint

To execute any websocket request one can "send" it to the url - `ws://{{host}}:{{port}}/websocket-endpoint`.

_Note:_

Make sure to replace the `host` and `port` of the running location service on local/remote machine.

## JSON Contract
* Request sample, response types and error types for every endpoints of all service is described in `allServiceData.json`
* `allServiceData.json` contains the single page summary of all services i.e. location-service, command-service, etc.
* Also Samples describing how to create requests, possible responses and possible errors can be found in same file.     

## Change

This is the base version - 1.0.0