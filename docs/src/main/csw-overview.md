# Using CSW

## OSW Architecture

## CSW Services
CSW or Common Software provides a shared software infrastructure based on a set of services and associated software for 
integrating individual components in the large observatory software architecture. The client applications use a set of 
loosely coupled services, each autonomous with a well-defined interface that hides the service implementation. It also   
provides a standardized communication with the services.
  
### [Location service](services/location.md)
The Location Service handles component (i.e., Applications, Sequencers, Assemblies, HCDs, and Services) registration 
and discovery in the distributed TMT software system.

### [Configuration service](services/config.md)
Configuration Service provides a centralized persistent store for any configuration file used in the TMT Software System. 
All versions of configuration files are retained, providing a historical record of each configuration file.

### [Logging service](services/logging.md)
Logging Service library provides an advanced logging facility for csw components and services.

### [Commands Service](command.md)
Commands service provide support for receiving, sending, and completing commands in the form of configurations

### [Framework](framework.md)
The framework provides templates for the kind of software components defined by TMT as well as service access interfaces.