# csw-config-client-cli

A command line application that facilitates interaction with config service. It accepts various commands to store, retrieve, list and manage configuration files.

## Supported Commands

Note: Exactly one operation must be specified from this list - `{create | update | get | delete | list | history | setActiveVersion | resetActiveVersion | getActiveVersion | getActiveByTime | getMetadata | exists | getActive}`

## Admin API
Below listed commands will be used by administrators and maintainers of Config service.

### create
creates the input file in the repository at a specified path

 * 'relativeRepoPath' is path in the repository
 * `-i`, `--in` is input file path
 * `--annex` is optional parameter. Add this option to specify if the input file must be save to annex store. It usually is the case if file is binary/large(>10 MiB)
 * `-c, --comment` optional create comment
 
### update
overwrites the file specified in the repository by the input file

 * 'path' is path in the repository
 * `-i`, `--in` is input file path
 * `-c, --comment` optional create comment
 
### get
retrieves a file for a given path, stored in config service, and writes it to the output file. Latest file is fetched if neither date nor id is specified.

 * 'relativeRepoPath' is path in the repository
 * `-o`, `--out` is output file path
 * `--id` optional parameter. version id of the repository file to get.
 * `--date` optional parameter. ex. 2017-04-16T16:15:23.503Z
 
### delete
 deletes the file at specified path in the repository
 
  * 'relativeRepoPath' is path in the repository
  * `-c, --comment` optional delete comment
  
### list
lists the files in the repository. Can't use '--annex' and '--normal' together.

 * `--annex` optional parameter. List all files that are of annex type. 
 * `--normal` optional parameter. List all files that are of normal type. 
 * `--pattern` optional parameter. List all files whose path matches the given pattern. e.g. "/path/hcd/*.*", "a/b/c/d.*", ".*.conf", ".*hcd.*"
 
### history
shows versioning history of the file in the repository

* 'relativeRepoPath' is path in the repository
* `--max` optional parameter. maximum no of files to be retrieved

### setActiveVersion
sets active version of the file in the repository

 * 'relativeRepoPath' is path in the repository
 * `--id` optional parameter. version id of the repository file to set as active.
 * `-c, --comment` optional delete comment

### resetActiveVersion
resets the active version to the latest version for the specified file

  * 'relativeRepoPath' is path in the repository
  * `-c, --comment` optional reset comment
  
### getActiveVersion
gets the id of the active version of the file in the repository

 * 'relativeRepoPath' is path in the repository
 
### getActiveByTime
gets the file that was active at a specified time

  * 'relativeRepoPath' is path in the repository
  * `-o`, `--out` is output file path
  * `--date` optional parameter. ex. 2017-04-16T16:15:23.503Z
  
### getMetadata
gets the metadata of config server e.g. repository directory, annex directory, min annex file size, max config file size

## Client API
Below listed commands are recommended for component developers.

### exists
checks if the file exists at specified path in the repository

 * 'relativeRepoPath' is path in the repository
 
### getActive
retrieves active file for a given path from config service, and writes it to the output file
  * 'relativeRepoPath' is path in the repository
  * `-o`, `--out` is output file path
  
## About this application 
 
### --help 
Prints the help message.

### --version 
Prints the version of the application.