# Authentication and Authorization Service (AAS)

Authentication and Authorization Service (AAS) is a suite of libraries (aka adapters) provided by CSW to help build an
ecosystem of client & server side applications that enforce authentication & authorization policies for TMT.

The backbone of AAS is the [Keycloak](https://www.keycloak.org/) product. It is where all client and server
applications need to be registered and configured. Keycloak comes bundled with `csw-services.sh` so you
don't have to download & install it manually. It is installed the first time you run `csw-services.sh`.

In the current authentication and authorization model for TMT, users will use browser-based graphical user
interfaces that will require authentication through AAS. Sequencers, Assemblies and HCDs execute within the
authenticated environment, but do not concern themselves directly with authorization and authentication.

 - @ref:[Core concepts & terms](aas/core-concepts-and-terms.md)
 - @ref:[Akka HTTP Adapter - csw-aas-http](aas/csw-aas-http.md)
 - @ref:[Installed Auth Adapter - csw-aas-installed](aas/csw-aas-installed.md)
 - @extref[Javascript Adapter - csw-aas-js](csw_js:aas/csw-aas-js)

## Technical Description
See @ref:[Authorization and Authentication Service Technical Description](../technical/aas/aas.md).

