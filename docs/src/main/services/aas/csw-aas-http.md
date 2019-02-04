# Akka HTTP Adapter (csw-aas-http)

This library is a security adapter for akka-http server applications. csw-aas uses 
[OpenId Connect](https://openid.net/connect/) for authentication and authorization.
The authentication server used by aas is [keycloak](https://www.keycloak.org/).
we recommend that you get familiar with keycloak's documentation and configurations to
fully leverage this adapters features.


This adapter provides authentication via security directives such as sGet, sPost, sPut, etc.
For authorization, these secure directives accept a wide range of policy expressions.

## Dependencies

To use the Akka HTTP Adapter (csw-aas-http), add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-aas-http" % "$version$"
    ```
    @@@
    
    
    
## Prerequisites

To run an akka-http server app, which uses this adapter, we need

* location service running
* Keycloak instance running and registered with location service

Location service and keycloak can be running on different machines. To start location service & keycloak 
server on a local machine, you make make use of csw-services.sh script.

## Application Configurations

All auth related configurations go inside `auth-config` block. There are tree configurations 
applicable for a akka-http server application i.e. `realm`, `client-id` & `enable-permissions`. 

`realm` has a default value of `TMT` if not specified. Ideally all apps in TMT should not have to override
this, however it might be useful to override this while testing your app.

`enable-permissions` is optional config with a default value of false. If your akka-http server application
uses permission based authorization policies, this config needs to set to true.

`client-id` is a mandatory configuration which specifies the client id of the app as per registration
in keycloak.

```hocon
auth-config {
  realm = TMT # DEFAULT
  enable-permissions = false # DEFAULT
  client-id = demo-cli # REQUIRED
} 
```
 

## Building a Secure Akka-HTTP server application

The core of this adapter is the `SecurityDirectives` class. The recommended way to instantiate 
`SecurityDirectives` is as shown below.

Scala
:   @@snip [SecurityDirectives](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #sample-http-app }

Importing everything from security directives is recommended as it imports some implicit 
methods along with all security directives.

In the above example, `GET http://localhost:9003/api` does not use any security directive and hence 
is accessible to all. `POST http://localhost:9003/api` uses `sPost` which is secure directive. This directive
takes care of authentication (access token signature & expiration validation).
For authorization it needs an [authorization policy](#authorization-policies). Authorizing policy specifies 
one or more conditions for request validation. 

In this instance, `sPost` directive has been given `RealmRolePolicy` policy with parameter value `admin`.

This results into following sequence of actions when a request arrives for a secure directive route

1. Secure directive will check request header to look for an access token
1. Validate token signature and expiry
1. Check the token for roles and validate that it has `admin` [realm role](https://www.keycloak.org/docs/latest/server_admin/index.html#realm-roles)
1. After all the above checks/validations pass, execute the route logic 

If any of the validations fails, an appropriate http status code is returned to the requester.
For authentication failure 401 is sent and for authorization failure 403 is sent.

@@@ note
To know more about realm roles, check out [keycloak documentation](https://www.keycloak.org/docs/latest/server_admin/index.html#realm-roles)
@@@

## Authorization Policies 

An authorization policy is a way to provide filter incoming HTTP requests based on standard rules. Following policies can
be applied to protect routes.

 - [ReamRolePolicy](#realmrolepolicy)
    Realm-level roles are a global namespace to define roles.
 - [ClientRolePolicy](#clientrolepolicy)
 - [PermissionPolicy](#permissionpolicy)
 - [CustomPolicy](#custompolicy)
 - [EmptyPolicy](#emptypolicy)

### RealmRolePolicy

This policy filters requests based on Realm Role. A Realm Role is global role within a realm and is applicable for all clients 
within realm.

In the following example policy will authorize request if user has assigned `admin`

Scala
:   @@snip [Realm Role Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #realm-role-policy }
 
### ClientRolePolicy

Client roles are basically a namespace dedicated to a client. Each client gets its own namespace.

This policy filters requests based on Client Role. In the following example policy will authorize 
request if user has assigned `accounts-admin` for clientId specified in configurations

Scala
:   @@snip [Client Role Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #client-role-policy }
 

### PermissionPolicy

This policy filters requests based on permissions. It expects name of scope and name of resource on which permission is created 
in keycloak server. Scope and Resource forms a "Permission". For example, "scope: Sell; resource: Vehicle" combined
specifies that the user with this scope and resource combination can "sell vehicles".

In the following example policy will authorize request if user has appropriate permission associated in keycloak which specifies
`delete` scope for `account` resource.

Scala
:   @@snip [Permission Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #permission-policy } 

### CustomPolicy

This policy allows custom request filtering based on access token properties. It expects predicate function which 
accepts an access token and returns a boolean. If the predicate returns true, it indicates user is authorized.

In the following example policy will authorize request if user's given name contains `test-user`

Scala
:   @@snip [Custom Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #custom-policy } 

### EmptyPolicy

This policy is used this when only authentication is needed but not authorization.

In the following example policy will pass if user is authenticated

Scala
:   @@snip [Empty Policy](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #empty-policy }
 

---------------- REVIEWED -------------------

## Writing Secure Routes

csw-aas-http adapter supports following secure HTTP verbs:

sGet        - Rejects all un-authorized and non-GET requests
sPost       - Rejects all un-authorized and non-POST requests
sPut        - Rejects all un-authorized and non-PUT requests
sDelete     - Rejects all un-authorized and non-DELETE requests
sHead       - Rejects all un-authorized and non-HEAD requests
sConnect    - Rejects all un-authorized and non-CONNECT requests

These security directives are instance methods of class `SecurityDirectives`. To use these directives, recommended approach is
to first create an instance of `SecurityDirectives` and import everything from it. Here's an example:

Scala
:   @@snip [SecurityDirectives](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #security-directive-usage }

These secure HTTP verbs expect authorization policy to use for filtering requests. These secure HTTP verbs and authorization
policies explained above can be used to achieve securing route.

Following example shows secure POST request which filters requests based on RealmRole policy. User having `example-admin-role`
is authorized to hit this POST route. 

Scala
:   @@snip [Secure Route](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #secure-route-example }

Following example shows akka http routes with some secure and open routes

Scala
:   @@snip [Routes](../../../../../examples/src/main/scala/csw/auth/ExampleServer.scala) { #example-routes }
 

## Source code for above examples

* @github[Example http server](/examples/src/main/scala/csw/auth/ExampleServer.scala)


# TODO

* permissions - setup - keycloak setup
* access token handle
* custom policy execution example
* expressions
