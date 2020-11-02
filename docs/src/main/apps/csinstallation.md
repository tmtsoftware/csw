# coursier installation
In TMT we have adopted coursier to setup scala/java environment on development/production machines and install any java based 
apps provided by csw and esw. 

## Coursier Installation and Setup
1.  Coursier binary needs to be present on the machine to proceed further with the installation.Please refer to the
necessary steps for downloading coursier launcher @link:[coursier installation](https://get-coursier.io/docs/cli-installation) { open=new }. 

2. The next step is to setup coursier.
    
    ```
    ./cs setup --jvm 11
    ```
    The above command does the following things:
    
    1.  It adds coursier bin where applications will be installed by default to PATH.
    2.  It installs Java AdoptOpenJDK 11.


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
// 2.0.1 is the version of the application to be installed
cs install location-server:2.0.1
```

The version number or SHA if avoided in the `install` command, `location-server` will be installed with the latest tagged binary of `csw-location-server`.
```bash
// location-server is the name of the application installed
cs install location-server
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

    
