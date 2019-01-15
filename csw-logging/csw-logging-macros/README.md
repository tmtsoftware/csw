macros
======

Logging service uses scala macros defined in this project to fetch location of log statement in a file i.e. fileName, packageName, 
className, line number.
To perform macro expansion, compiler needs a macro implementation in executable form. Thus macro implementations need to 
be compiled before the main compilation, which requires macros to be placed in a separate project.