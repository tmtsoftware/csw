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

 - [TMTAuthContextProvider](#TMTAuthContextProvider)
 - [Consumer](#consumer)
 - [Login](#login)
 - [Logout](#logout)
 - [CheckLogin](#checklogin)
 - [RealmRole](#realmrole)
 - [ClientRole](#clientrole)

## TMTAuthContextProvider

TMTAuthContextProvider is wrapper over provider from react context API. It expects config json to be passed. This config 
json is UI application specific AAS server configuration e.g. clientId, realm. When user loges in AAS Server is instantiated 
by merging UI application specific config and predefined configuration. UI application can choose to override predefined 
json configuration. Once AAS sever is instantiated, tmtAuth object is updated with needed attributes and apis. This tmtAuth
is available to all react componets . tmtAuth Context is designed to share data that can be considered “global” for a 
tree of React components and is available via Consumer. All consumers that are descendants of a Provider will re-render 
whenever the TMTAuthContextProvider’s state changes i.e tmtAuth. Recommended way is use TMTAuthContextProvider to wrap entire 
application so that data can be shared anywhere in application via Consumer. 

Javascript
:   @@snip [TMTAuthContextProvider.jsx](../../../../../csw-aas/csw-aas-js/example/src/components/ExampleApp.jsx) { #TMTAuthContextProvider-component-usage }

### Source code for RealmRole component

* @github[TMTAuthContextProvider Component](/csw-aas/csw-aas-js/src/components/context/TMTAuthContextProvider.jsx)

## Consumer

Consumer is similar to consumer from react context api. tmtAuth can be accessed using Consumer component 

Javascript
:   @@snip [Consumer.jsx](../../../../../csw-aas/csw-aas-js/example/src/components/Read.jsx) { #Consumer-component-usage }

### Source code for RealmRole component

* @github[Consumer Component](/csw-aas/csw-aas-js/src/components/context/TMTAuthContext.jsx)

## Login

Login component instantiate AAS server with configurations provided. It redirects to AAS server login page for user to login.
After login tmtAuth in context is updated with appropriate values e.g. token, realm & client roles etc.

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

## ClientRole

ClientRole components provides ability to show something only if user is logged in and has specified client role for 
specified client. In the following code snippet div is shown only if user is logged in and has client role specified 
in clientRole prop for client specified in client prop.
Behaviour is user is not logged in can be optionally defined by html element or react component and that can be passed to error prop
e.g - ExampleError Component in following snippet.

Javascript
:   @@snip [ClientRole.jsx](../../../../../csw-aas/csw-aas-js/example/src/components/ExampleApp.jsx) { #clientRole-component-usage }

### Source code for ClientRole component

* @github[ClientRole Component](/csw-aas/csw-aas-js/src/components/authorization/ClientRole.jsx)
