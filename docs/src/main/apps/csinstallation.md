# coursier-installation

## Coursier Installation
Please refer to the necessary steps for installation @link:[coursier-installation](https://get-coursier.io/docs/cli-installation) { open=new }. 

## Coursier Setup
Please run the command `./cs setup --jvm 11 ` to add cs bin where applications will be installed by default to PATH.

@@@ note

Please refer to the following guide for more details regarding setup: @link:[coursier-setup](https://get-coursier.io/docs/cli-setup) { open=new }.

@@@

## Add TMT Apps channel to your local Coursier installation using below command

Channel needs to be added to install application using `cs install`

For developer machine setup,

```bash
cs channel --add https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

For production machine setup,

```bash
cs channel --add https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

## General Examples

### Installing an application
The  command `cs install <app-name>:<version | SHA>`can be used to install an application using coursier.  
```bash
// location-server is the name of the application installed
// 1.0.0 is the version of the application to be installed
cs install location-server:1.0.0
```
### Help
Use the following command to get help on the options available with this app.
```bash
   location-server --help
```

### Version
Use the following command to get version information for the app.
  
```bash
   location-server --version
```    

    
