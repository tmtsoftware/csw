# csw-config-client-cli

A command line application that facilitates interaction with config service. It accepts various commands to store, manage and retrieve configuration files.

## Commands

Note: Exactly one command must be specified from the list - `{create | update | get | exists | delete | list | history | setDefault | resetDefault | getDefault}`

### create
creates the input file in the repository at a specified path

 * 'relativeRepoPath' is path in the repository
 * `-i`, `--in` is input file path
 * `--oversize` is optional parameter. Add this option to specify if the input file is large/binary files
 * `-c, --comment` optional create comment
 
### update
overwrites the file specified in the repository by the input file

 * 'path' is path in the repository
 * `-i`, `--in` is input file path
 * `-c, --comment` optional create comment
 
### get
retrieves a file for a given path, stored in config service, and writes it to the output file

 * 'relativeRepoPath' is path in the repository
 * `-o`, `--out` is output file path
 * `--id` optional parameter. version id of the repository file to get.
 * `--date` optional parameter. ex. 2017-04-16T16:15:23.503Z

### exists
checks if the file exists at specified path in the repository

 * 'relativeRepoPath' is path in the repository

### delete
deletes the file at specified path in the repository

 * 'relativeRepoPath' is path in the repository

### list
lists the files in the repository

### history
shows versioning history of the file in the repository

 * 'relativeRepoPath' is path in the repository
 * `--max` optional parameter. maximum no of files to be retrieved
 
### setDefault
sets default version of the file in the repository

 * 'relativeRepoPath' is path in the repository
 * `--id` optional parameter. version id of file to be set as default.
 
### resetDefault
resets the default to the latest version of the file in the repository

 * 'relativeRepoPath' is path in the repository

### getDefault
gets the default version of the file in the repository

 * 'relativeRepoPath' is path in the repository
 * `-o`, `--out` is output file path

### --help 
Prints the help message.

###--version 
Prints the version of the application.

## Examples

In progress...