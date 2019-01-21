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

csw-aas-js exposes following react components: 

@@ toc { .main depth=1 }

@@@ index
 - [Login](./csw-aas-js.md#Login)
 - [Logout]
 - [CheckLogin]
 - [RealmRole]
 - [ResourceRole]
 - [TMTAuthContextProvider]
 - [Consumer]
@@@

### Login

Login component instantiate AAS server with configurations provided. It redirects to AAS server login page for user to login.

Javascript
:   @@snip [Login.jsx](../../../../examples/src/main/scala/csw/database/AssemblyComponentHandlers.scala) { #dbFactory-write-access }





