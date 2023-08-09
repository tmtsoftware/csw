# Authentication and Authorization Service (AAS)

THe Authentication and Authorization Service (AAS) is a suite of libraries (i.e. adapters) provided by CSW to help build an
ecosystem of client & server side applications that enforce the authentication and authorization policies for TMT.

The backbone of AAS is the [Keycloak](https://www.keycloak.org/) product. It is where all client and server
applications need to be registered and configured. Keycloak comes bundled with `csw-services` application, so you
don't have to download & install it manually. Refer @ref:[here](../apps/cswservices.md) to start AAS service with `csw-services`.

In the current authentication and authorization model for TMT, users will use browser-based graphical user
interfaces to perform authentication through AAS. Sequencers, Assemblies and HCDs execute within the
authenticated environment, but do not concern themselves directly with authorization and authentication.

 - @ref:[Core concepts & terms](aas/core-concepts-and-terms.md)
 - @ref:[Pekko HTTP Adapter - csw-aas-http](aas/csw-aas-http.md)
 - @ref:[Installed Auth Adapter - csw-aas-installed](aas/csw-aas-installed.md)
 - @extref[Javascript Adapter - ESW-TS](esw_ts:aas/auth-components.html)

## Technical Description
See @ref:[Authorization and Authentication Service Technical Description](../technical/aas/aas.md).

