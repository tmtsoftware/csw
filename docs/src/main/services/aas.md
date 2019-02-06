# Authentication and Authorization Service (AAS)

Authentication and Authorization Service (AAS) is a suite of libraries (aka adapters) provided CSW to help build 
ecosystem of client & server side applications that enforce authentication & authorization policies.

The backbone of AAS is [keycloak](https://www.keycloak.org/). It is where all the client and server
applications need to registered and configured. keycloak comes bundled with `csw-services.sh` so you
don't have to download & install it manually.

@@ toc { .main depth=1 }

@@@ index
 - [Core concepts & terms](aas/core-concepts-and-terms.md)

 - [Akka HTTP Adapter - csw-aas-http](aas/csw-aas-http.md)
    
 - [Javascript Adapter - csw-aas-js](aas/csw-aas-js.md)
    
 - [Native Auth Adapter - csw-aas-native](aas/csw-aas-native.md)
@@@