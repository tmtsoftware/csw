# Prefix in CSW 2.0.0

The use of prefixes has been made more consistent in CSW 2.0.0.  Now, they 

Prefix no longer takes a String and extracts the Subsystem from it.  It now must explicity be constructed with a Subsystem type.

Prefix and Subsystem moved from csw.params.core.models to csw.prefix.models

Prefix was a dot-separated String starting with a subsystem, where the component name was considered to be the last item.

e.g IRIS.imager.filterWheelAssembly =>
  Subsystem: IRIS
  Component Name: filterWheelAssembly

Now, prefix is made from a Subsystem type and a String component name, where everything after the subsystem is considered to 
be the component name:

e.g. IRIS.imager.filterWheelAssembly =>
  Subsystem: IRIS
  Component Name: imager.filterWheelAssembly
  
  

