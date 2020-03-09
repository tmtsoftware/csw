# Migration Guide from 1.0.0 to 2.0.0

This guide shows how to migrate from CSW Version 1.0.0 to CSW Version 2.0.0. From the release notes, the following
are the important changes from 1.0.0:

* Simplified CommandResponseManager and removed auto-completion of commands
* Prefix has Subsystem in constructor
* Log statements have subsystem and prefix along with componentName
* AlarmKey and ComponentKey is constructed from prefix instead of string
* TcpLocation and HttpLocation has prefix along with AkkaLocation
* ComponentType is displayed to snake_case from lowercase
* Subsystem is displayed in uppercase instead of lowercase
* ArrayData and MatrixData does not require classtag for creation
* Admin routes for setting log level and getting log level are now available via gateway
* JSON contracts for location and command service added in paradox documentation

Details on how to work with the updated Prefix type are given @ref[here](prefix.md).

Details on how to work with the new Command Service changes are given @ref[here](commandService.md).

