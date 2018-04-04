# csw-config-client-cli

A command line application that facilitates interaction with Configuration Service. It accepts various commands to store, retrieve, list and manage configuration files.

## Supported Commands

Note: Exactly one operation must be specified from this list - `{create | update | get | delete | list | history | setActiveVersion | resetActiveVersion | getActiveVersion | getActiveByTime | getMetadata | exists | getActive}`

## Admin API
The commands listed below will be used by administrators and maintainers of Configuration Service.

### create
Takes an input source file and creates the configuration in the repository at a specified path.

 * 'relativeRepoPath' is path in the repository
 * `-i`, `--in` is input file path
 * `--annex` is optional parameter. Add this option to specify if the input file must be save to annex store. It usually is the case if file is binary/large(>10 MiB)
 * `-c, --comment` optional create comment
 
### update
Overwrites the file specified in the repository by the input file.

 * 'path' is path in the repository
 * `-i`, `--in` is input file path
 * `-c, --comment` optional create comment
 
### get
Retrieves a file for a given path and saves it to the output file. The latest file is fetched if neither date nor id is specified.

 * 'relativeRepoPath' is path in the repository.
 * `-o`, `--out` is output file path
 * `--id` optional. if specified this id will be matched
 * `--date` optional. if specified will get the file matching this date. Format: 2017-04-16T16:15:23.503Z
 
### delete
Deletes the file at the specified path in the repository.
 
  * 'relativeRepoPath' is path in the repository
  * `-c, --comment` optional delete comment
  
### list
Lists the files in the repository. Can't use '--annex' and '--normal' together.

 * `--annex` optional parameter. List all files that are of annex type. 
 * `--normal` optional parameter. List all files that are of normal type. 
 * `--pattern` optional parameter. List all files whose path matches the given pattern. e.g. "/path/hcd/*.*", "a/b/c/d.*", ".*.conf", ".*hcd.*"
 
### history
Shows the version history of the file in the repository.

* 'relativeRepoPath' is path in the repository
* `--max` optional parameter. maximum no of files to be retrieved

### setActiveVersion
Sets the active version of the file in the repository.

 * 'relativeRepoPath' is path in the repository
 * `--id` optional parameter. version id of the repository file to set as active.
 * `-c, --comment` optional delete comment

### resetActiveVersion
Resets the active version to the latest version for the specified file path.

  * 'relativeRepoPath' is path in the repository
  * `-c, --comment` optional reset comment
  
### getActiveVersion
Gets the id of the active version of the file in the repository.

 * 'relativeRepoPath' is path in the repository
 
### getActiveByTime
Gets the file that was active at a specified time.

  * 'relativeRepoPath' is path in the repository
  * `-o`, `--out` is output file path
  * `--date` optional. if specified will get the active file matching this date. Format: 2017-04-16T16:15:23.503Z
  
### getMetadata
Gets the metadata of Configuration Service server e.g. repository directory, annex directory, min annex file size, max config file size.

## Client API
The following commands are available for component developers.

### exists
Checks if the file exists at specified path in the repository.

 * 'relativeRepoPath' is path in the repository
 
### getActive
Retrieves active file for a given path from Configuration Service and writes it to the output file.
  * 'relativeRepoPath' is path in the repository
  * `-o`, `--out` is output file path
  
## About this application 
 
### --help 
Prints the help message.

### --version 
Prints the version of the application.

## Examples

1. 
```
csw-config-client-cli create /path/hcd/trombone.conf -i /Users/admin/configs/trombone.conf -c "Initial version"
```   
Creates a config file at path `/path/hcd/trombone.conf`, using local file at `/Users/admin/configs/trombone.conf`, with `Initial version` as a comment.

2. 
```
csw-config-client-cli create /path/hcd/trombone.conf -i /Users/admin/large-configs/bigBinary.conf --annex
```   
Creates a config file at path `/path/hcd/trombone.conf`, using local file at `/Users/admin/large-configs/bigBinary.conf`, file will be stored in annex store.

3. 
```
csw-config-client-cli update /path/hcd/trombone.conf -i /Users/foo/new_trombone.conf -c "new conf for next observation"
```    
Updates repository file `/path/hcd/trombone.conf`, with a local file at `/Users/foo/new_trombone.conf`, using a specified comment.

4. 
```
csw-config-client-cli get /path/hcd/trombone.conf -o /Users/bar/temp/latest_trombone.conf
```    
Gets repository file `/path/hcd/trombone.conf`, stores at local disk location `/Users/bar/temp/latest_trombone.conf`

5. 
```
csw-config-client-cli get /path/hcd/trombone.conf -o /Users/bar/temp/old_trombone.conf --id 10
```    
Gets version revision 10 of repository file `/path/hcd/trombone.conf`, stores at local disk location `/Users/bar/temp/old_trombone.conf`

6. 
```
csw-config-client-cli getActive /path/hcd/trombone.conf -o /Users/bar/temp/scheduled_trombone.conf
```    
Gets currently active version of repository file `/path/hcd/trombone.conf`, stores at local disk location `/Users/bar/temp/scheduled_trombone.conf`

7. 
```
csw-config-client-cli exists /path/hcd/trombone.conf
```    
True if repository file `/path/hcd/trombone.conf` exists, false otherwise

8. 
```
csw-config-client-cli delete /path/hcd/outdated_trombone.conf -c monthly maintainance activity
```    
Deletes repository file `/path/hcd/outdated_trombone.conf` if it exists using a comment

9. 
```
csw-config-client-cli history /path/hcd/trombone.conf --max 25
```    
Prints history of repository file `/path/hcd/trombone.conf`, with only 25 entries.

10. 
```
csw-config-client-cli setActiveVersion /path/hcd/trombone.conf --id 4 -c restoring last successful version.
```    
Sets revision 4 to be active for repository file `/path/hcd/trombone.conf`, using a comment.

11. 
```
csw-config-client-cli resetActiveVersion /path/hcd/trombone.conf -c testing most recent config
```    
Sets latest revision to be active for repository file `/path/hcd/trombone.conf`, using a comment.

12. 
```
csw-config-client-cli getActiveVersion /path/hcd/trombone.conf
```    
Gets active version ID for repository file `/path/hcd/trombone.conf`.

13. 
```
csw-config-client-cli getActiveByTime /path/hcd/trombone.conf -o /usr/tmp/last_week_trombone.conf --date 2017-05-09T07:29:53.242Z
```    
Gets version of repository file `/path/hcd/trombone.conf`, that was active on `2017-05-09T07:29:53.242Z` and saves it on local disk.

14. 
```
csw-config-client-cli getMetadata
```    
Prints the metadata on screen.
