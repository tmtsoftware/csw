# Authentication and Authorization Service

### Introduction

The Authentication and Authorization Service provides suite of libraries which help to enforce authentication and authorization 
for web applications and CLI applications in the TMT ecosystem.

It provides following libraries (aka adapters). For information on _using_ these adapters, refer to following sections:

- @ref:[Pekko HTTP Adapter - csw-aas-http](../../services/aas/csw-aas-http.md) - enables security for Pekko HTTP server applications 
- @ref:[Installed Auth Adapter - csw-aas-installed](../../services/aas/csw-aas-installed.md) - enables security for CLI applications 
- @extref[Javascript Adapter - ESW-TS](esw_ts:aas/auth-components.html) - enables security for Javascript React applications

These adapters are written on top of adapters still provided by Keycloak-25.x.
Note that these adapters are missing from newer versions of Keycloak, so this code will need to be updated at some point.

![AAS Adapters](aas-adapters.png) 

For more information on the internals of these adapters, refer to the following sections:

- @ref:[Pekko HTTP Adapter - csw-aas-http](./csw-aas-http.md) 
- @ref:[Installed Auth Adapter - csw-aas-installed](./csw-aas-installed.md) 
- @extref[Javascript Adapter - ESW-TS](esw_ts:aas/auth-components.html)
