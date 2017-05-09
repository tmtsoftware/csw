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
retrieves a file for a given path and saves it to the output file. Latest file is fetched if neither date nor id is specified.

 * 'relativeRepoPath' is path in the repository
 * `-o`, `--out` is output file path
 * `--id` optional. if specified this id will be matched
 * `--date` optional. if specified will get the file matching this date. Format: 2017-04-16T16:15:23.503Z
 
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
  * `--date` optional. if specified will get the active file matching this date. Format: 2017-04-16T16:15:23.503Z
  
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

## Examples

**Example:** csw-config-client-cli "create" "/path/hcd/trombone.conf" -i "/Users/admin/configs/trombone.conf" -c "Initial version"   
**Explanation:** Creates a config file at path '/path/hcd/trombone.conf', using local file at "/Users/admin/configs/trombone.conf", with "Initial version" as a comment.

**Example:** csw-config-client-cli "create" "/path/hcd/trombone.conf" -i "/Users/admin/large-configs/bigBinary.conf" --annex   
**Explanation:** Creates a config file at path '/path/hcd/trombone.conf', using local file at "/Users/admin/large-configs/bigBinary.conf", file will be stored in annex store.

**Example:** csw-config-client-cli "update" "/path/hcd/trombone.conf" -i "/Users/foo/new_trombone.conf" -c "new conf for next observation"    
**Explanation:** Updates repository file "/path/hcd/trombone.conf", with a local file at "/Users/foo/new_trombone.conf", using a specified comment.

**Example:** csw-config-client-cli "get" "/path/hcd/trombone.conf" -o "/Users/bar/temp/latest_trombone.conf"    
**Explanation:** Gets repository file "/path/hcd/trombone.conf", stores at local disk location "/Users/bar/temp/latest_trombone.conf"

**Example:** csw-config-client-cli "get" "/path/hcd/trombone.conf" -o "/Users/bar/temp/old_trombone.conf" --id "10"    
**Explanation:** Gets version revision 10 of repository file "/path/hcd/trombone.conf", stores at local disk location "/Users/bar/temp/old_trombone.conf"

**Example:** csw-config-client-cli "getActive" "/path/hcd/trombone.conf" -o "/Users/bar/temp/scheduled_trombone.conf"    
**Explanation:** Gets currently active version of repository file "/path/hcd/trombone.conf", stores at local disk location "/Users/bar/temp/scheduled_trombone.conf"

**Example:** csw-config-client-cli "exists" "/path/hcd/trombone.conf"    
**Explanation:** True if repository file "/path/hcd/trombone.conf" exists, false otherwise

**Example:** csw-config-client-cli "delete" "/path/hcd/outdated_trombone.conf" -c "monthly maintainance activity"    
**Explanation:** Deletes repository file "/path/hcd/outdated_trombone.conf" if it exists using a comment

**Example:** csw-config-client-cli "history" "/path/hcd/trombone.conf" "--max" 25    
**Explanation:** Prints history of repository file "/path/hcd/trombone.conf", with only 25 entries.

**Example:** csw-config-client-cli "setActiveVersion" "/path/hcd/trombone.conf" --id "4" -c "restoring last successful version."    
**Explanation:** Sets revision 4 to be active for repository file "/path/hcd/trombone.conf", using a comment.

**Example:** csw-config-client-cli "resetActiveVersion" "/path/hcd/trombone.conf" -c "testing most recent config"    
**Explanation:** Sets latest revision to be active for repository file "/path/hcd/trombone.conf", using a comment.

**Example:** csw-config-client-cli "getActiveVersion" "/path/hcd/trombone.conf"    
**Explanation:** Gets active version ID for repository file "/path/hcd/trombone.conf".

**Example:** csw-config-client-cli "getActiveByTime" "/path/hcd/trombone.conf" -o "/usr/tmp/last_week_trombone.conf" --date "2017-05-09T07:29:53.242Z"    
**Explanation:** Gets version of repository file "/path/hcd/trombone.conf", that was active on "2017-05-09T07:29:53.242Z" and saves it on local disk.

**Example:** csw-config-client-cli "getMetadata"    
**Explanation:** Prins the metadata on screen.

