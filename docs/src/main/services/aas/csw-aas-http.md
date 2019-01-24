# Akka HTTP Adapter (csw-aas-http)

csw-aas-http is akka http adapter.To know more about akka http please refer akka-http documentation. It provides dsl to 
write secured http routes. DSL provides secure http verbs e.g. sGet, sPost etc.This secure route dsl accepts authorization 
policy which is evaluated against access token string passed in HTTP request. It expects AAS server running. AAS server 
can be started using csw-services.sh script. Keycloak is being used as AAS server. To know more about Keycloak please refer 
this [link](https://www.keycloak.org/documentation.html)

<!-- introduction to the service -->

## Dependencies

To use the Akka HTTP Adapter (csw-aas-http), add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-aas-http" % "$version$"
    ```
    @@@
    
## Getting Started

Start csw services using csw-services.sh script. It will start AAS server and register with location service.

 - Configurations
 - Authorization Policies
 - Writing Secure Routes
 - Example HTTP Server
 
## Configurations

To write akka http server using csw-aas-http adapter following configuration are needed in application.conf file for
http server.

Scala
:   @@snip [Configurations](../../../../../examples/src/main/resources/application.conf) { #auth-config }

For this server one bearer only client needs to be created in Keyclok. For more details regarding realm and resource please refer 
this [link](https://www.keycloak.org/documentation.html)
realm - Realm being used
client-id - clientId in Keycloak 
enable-permissions - if permission policy is being used in http routes then this flag needs to be enabled. Default value 
for this is false (disabled). If permission policy is used and this flag is not enabled then even user has specified permissions
for performing action will get 403 forbidden error code.
 
## Authorization Policies 

An authorization policy is a way to provide filter incoming HTTP requests based on standard rules. Following policies can
be applied to protect routes.

 - [ClientRolePolicy](#clientrolepolicy)
 - [ReamRolePolicy](#realmrolepolicy)
 - [PermissionPolicy](#permissionpolicy)
 - [CustomPolicy](#custompolicy)
 - [EmptyPolicy](#emptypolicy)

## ClientRolePolicy

This policy filters requests based on Client Role. A Client Role is a client specific role.

In the following example policy will authorize request if user has assigned `person-role` for clientId specified in configurations

Scala
:   @@snip [Client Role Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #client-role-policy }
 

## RealmRolePolicy

This policy filters requests based on Realm Role. A Realm Role is global role within a realm and is applicable for all clients 
within realm.

In the following example policy will authorize request if user has assigned `example-admin-role`

Scala
:   @@snip [Realm Role Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #realm-role-policy }
 

## PermissionPolicy

This policy filters requests based on permissions. It expects name of scope and name of resource on which permission is created 
in keycloak server.

In the following example policy will authorize request if user has appropriate permission associated in keycloak which specifies
`delete` scope for `person` resource.

Scala
:   @@snip [Permission Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #permission-policy }
 

## CustomPolicy

This policy allows custom request filtering based on access token properties. It expects filter condition

In the following example policy will authorize request if user's given name contains `test-user`

Scala
:   @@snip [Custom Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #custom-policy }
 

## EmptyPolicy

This policy is used this when only authentication is needed but not authorization.

In the following example policy will pass if user is authenticated

Scala
:   @@snip [Empty Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #empty-policy }
 

## Writing Secure Routes

csw-aas-http adapter supports following secure HTTP verbs:

sGet        - Rejects all un-authorized and non-GET requests
sPost       - Rejects all un-authorized and non-POST requests
sPut        - Rejects all un-authorized and non-PUT requests
sDelete     - Rejects all un-authorized and non-DELETE requests
sHead       - Rejects all un-authorized and non-HEAD requests
sConnect    - Rejects all un-authorized and non-CONNECT requests

These secure HTTP verbs expect authorization policy to use for filtering requests. These secure HTTP verbs and authorization
policies explained above can be used to achieve securing route.

Following example shows secure POST request which filters requests based on RealmRole policy. User having `example-admin-role`
is authorized to hit this POST route. 

Scala
:   @@snip [Secure Route](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #secure-route-example }

Following example shows akka http routes with some secure and open routes

Scala
:   @@snip [Routes](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #example-routes }
 
 
## Example HTTP Server

Points to remember while writing http server with secure routes

* Instantiate SecurityDirectives with appropriate config. SecurityDirectives exposes factory to instantiate SecurityDirectives
based on config or using location service. 

Following example shows one way of instantiating SecurityDirectives

Scala
:   @@snip [SecurityDirectives](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #security-directive-usage }

Please refer API documentation for more information regarding SecurityDirectives.

This enables usage of secure HTTP verbs while writing routes

## Source code for example http server

* @github[Example http server](/examples/src/main/scala/csw/auth/ExampleServer.scala)


