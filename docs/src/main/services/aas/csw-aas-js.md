# Javascript Adapter (csw-aas-js)

csw-aas-js is a npm package which provides react components. UI applications can use these react components which will 
enable UI application to show or hide components based on authentication and authorization policy.  

<!-- introduction to the javascript adapter -->

## Dependencies

To use the csw-aas-js adapter, run this command from root folder of your application where package.json exists:

npm
:   @@@vars
    ```javascript
        npm i --save csw-aas-js@$version$
    ```
    @@@
    
yarn
:   @@@vars
    ```javascript
        yarn add csw-aas-js@$version$
    ```
    @@@
    
## Components

csw-aas-js exposes react components. 

Javascript
:   @@snip [Exported Components](../../../../../csw-aas/csw-aas-js/src/aas.js) { #export-components }


Importing and usage of those components is explained below:
### Importing components

Components can be imported as shown in code snippet below

Javascript
:   @@snip [Import Components](../../../../../csw-aas/csw-aas-js/example/src/components/NavComponent.jsx) { #import-components }

csw-aas-js exposes following react components: 

 - [Login](#login)
 - [Logout](#logout)
 - [CheckLogin](#checklogin)
 - [RealmRole](#realmrole)
 - [ResourceRole](#resourcerole)
 - [TMTAuthContextProvider]
 - [Consumer]



## Login

Login component instantiate AAS server with configurations provided. It redirects to AAS server login page for user to login.
After login tmtAuth in context is updated with appropriate values e.g. token, realm & resource roles etc.

Javascript
:   @@snip [Login.jsx](../../../../../csw-aas/csw-aas-js/example/src/components/NavComponent.jsx) { #login-component-usage }

### Source code for Login component

* @github[Login Component](/csw-aas/csw-aas-js/src/components/Login.jsx)

## Logout

Logout component logs out user from AAS server. It clears tmtAuth stored in context.

Javascript
:   @@snip [Logout.jsx](../../../../../csw-aas/csw-aas-js/example/src/components/NavComponent.jsx) { #logout-component-usage }

### Source code for Logout component

* @github[Logout Component](/csw-aas/csw-aas-js/src/components/Logout.jsx)

## CheckLogin

CheckLogin components provides ability to show something only if user is logged in. 
In the following code snippet Write is a react component is shown only if user is logged in.
Behaviour is user is not logged in can be defined by html element or react component and that can be passed to error prop
e.g - ExampleError Component in following snippet

Javascript
:   @@snip [CheckLogin.jsx](../../../../../csw-aas/csw-aas-js/example/src/components/ExampleApp.jsx) { #checkLogin-component-usage }

### Source code for CheckLogin component

* @github[CheckLogin Component](/csw-aas/csw-aas-js/src/components/authentication/CheckLogin.jsx)

## RealmRole

RealmRole components provides ability to show something only if user is logged in and has specified realm role. 
In the following code snippet div is shown only if user is logged in and has realm role specified in realmRole prop
Behaviour is user is not logged in can be optionally defined by html element or react component and that can be passed to 
error prop e.g - ExampleError Component in following snippet.

Javascript
:   @@snip [RealmRole.jsx](../../../../../csw-aas/csw-aas-js/example/src/components/ExampleApp.jsx) { #realmRole-component-usage }

### Source code for RealmRole component

* @github[RealmRole Component](/csw-aas/csw-aas-js/src/components/authorization/RealmRole.jsx)

## ResourceRole

ResourceRole components provides ability to show something only if user is logged in and has specified resource role for 
specified resource. In the following code snippet div is shown only if user is logged in and has resource role specified 
in resourceRole prop for resource specified in resource prop.
Behaviour is user is not logged in can be optionally defined by html element or react component and that can be passed to error prop
e.g - ExampleError Component in following snippet.

Javascript
:   @@snip [ResourceRole.jsx](../../../../../csw-aas/csw-aas-js/example/src/components/ExampleApp.jsx) { #resourceRole-component-usage }

### Source code for RealmRole component

* @github[RealmRole Component](/csw-aas/csw-aas-js/src/components/authorization/RealmRole.jsx)
