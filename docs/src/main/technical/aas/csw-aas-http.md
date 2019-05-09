# csw-aas-http - Akka HTTP Adapter 

This is security adapter for akka http server applications. It exposes security directives e.g. sGet, sPost etc which enforce
authentication and authorization based on authorization policies.

In order for akka http server to utilize keycloak it has to be registered as keycloak client. Please refer to [keycloak documentation](https://www.keycloak.org/docs/latest/getting_started/index.html)
for details.

## Types of tokens

###ID Token - 
The ID Token is a JSON Web Token (JWT) that contains user profile information (such as the user's name and email) 
which is represented in the form of claims.

###Access Token - 
An Access Token is a credential that can be used by an application to access an API. Access Tokens is JSON web token digitally 
signed using JSON web signature (JWS). They inform the API that the bearer of the token has been authorized to access the API and perform 
specific actions specified by the scope that has been granted. Access token contains all the information that ID token has. 
Additionally it has information related to realm and resource roles associated to user. This information is used for authorization 
based on clientRole and RealmRole policy.

###Requesting Party Token - 
A requesting party token (RPT) is a JSON web token (JWT) digitally signed using JSON web signature (JWS). 
The token is built based on the OAuth2 access token previously issued by Keycloak to a specific client acting on behalf 
of a user or on its own behalf. RPT contains all the information which access token has, additionally it has information
related to permissions granted. permissions are directly related with the resources/scopes you are protecting. This is used for
authorization based on permission policy.  

## Request flow 

When request comes to secure akka http server, it performs following steps.

- Authentication 

Authentication involves token verification and decoding. Secure http endpoints expect access token to be provided in request header. 
For verification it uses api provided by keycloak-adapter-core. For decoding csw-aas-http uses third party library - [jwt-play-json](https://github.com/pauldijou/jwt-scala).
If "enable-permissions" flag is enabled, it involves additional call to keycloak server for fetching RPT using access token provided
in request header. RPT is then decoded using jwt-play-json. Authentication process verifies access token string and decode 
it into `AccessToken` domain model. 

- Authorization

Authorization involves applying specified `AuthorizationPolicy` against `AccessToken`. Foe example, role based authorization 
involves checking access to secure api against roles information present in access token. Permission based authorization 
involves checking access to secure api against permissions present in RPT.
 
Following diagrams shows request flow for secure akka http server.

![aas-http-flow.png](akka-http-workflow.png) 

## Asynchronous nature of Akka-HTTP Routing layer

csw-aas-http uses `authenticateOAuth2Async` and `authorizeAsync` which are async variants of akka-http security directives. This allows 
it to run without blocking routing layer of Akka HTTP, freeing it for other requests. Similarly to maintain asynchronous nature
csw-aas-http also wraps blocking calls for keycloak adapter token verifier and call for fetching RPT from keycloak inside `Future`.
