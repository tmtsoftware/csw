package csw

/**
 * == Configuration Server ==
 *
 * The config server is a HTTP based implementation which exposes HTTP routes for CRUD operations
 * to manage configuration files by storing them in a Subversion repository,
 * however the Config Client wrapper implements the ConfigService trait
 * and provides a programmatic API to access config server and perform CRUD operations.
 *
 * === Config Server Application ===
 *
 * Before starting the config server, the ''cluster seed application'' should be running on a known port and IP address.
 * You can start the config server with the `csw-config-server` command (found under target/universal/stage/bin) by passing command line arguments.
 * The locations of the repositories are defined in resources/application.conf.
 * You can also override the values with system properties. For example:
 *
 * {{{
 *      csw-config-server -Dcsw-config-server.repository-dir=/myPath/MyLocalRepo
 *                        -Dcsw-config-server.annex-dir=/myPath/MyAnnexRepo
 * }}}
 *
 *  Config Server application takes following command line arguments:
 *  `--initRepo`  It’s an optional parameter. When supplied, server will try to initialize a repository if it does not exist.
 *  `--port`      It’s an optional parameter. When specified the HTTP server will start on this port. Default will be 4000.
 *  `--help`      Prints the help message.
 *  `--version`   Prints the version of the application.
 *
 * Example: `csw-config-server –initRepo`
 * Explanation: Start HTTP config server on default port 4000. Initialize repository if it does not exist and register's it with LocationService
 *
 * === Config Service Http Routes ===
 *
 * The `create` and `update` methods expect the file data to be posted.
 * The `path` segment parameter is always required. All other query arguments are optional.
 * The `id` argument (a `ConfigId`) must be taken from the JSON result of `create`, `update`, `list`, or `history`.
 *
 * The format of the JSON returned from `create` and `update` is:
 * `{"id":"1"}`. The `id` query argument passed to the other methods is
 * the value at right.
 *
 * === Example or using curl to access the Config Service Http Server ===
 *
 * Assuming that the config service http server is running on localhost on port 4000 (see config file, default: application.conf):
 *
 * {{{curl -X POST 'http://localhost:4000/config/test1/TestConfig1.conf?comment=comment+here' --data-binary @TestConfig1}}}
 *
 * Creates a new file in the config service named /test1/TestConfig1 using the data in the local file TestConfig1.
 *
 * {{{curl 'http://localhost:4000/config/test1/TestConfig1' > TestConfig1a}}}
 *
 * Gets the contents of some/test1/TestConfig1 from the service and store in a local file.
 *
 * {{{curl -X PUT 'http://localhost:4000/config/test1/TestConfig1?comment=some+comment' --data-binary @TestConfig1}}}
 *
 * Updates the contents of some/test1/TestConfig1 in the config service with the contents of the local file.
 *
 * {{{curl -s 'http://localhost:4000/history/test1/TestConfig1'}}}
 *
 * Returns JSON describing the history of some/test1/TestConfig1. You can pipe the output to json_pp to pretty print it:
 *
 * {{{
 *
 *      [
 *          {
 *              "id": {
 *                  "id": "3"
 *              },
 *              "comment": "Update 2 comment",
 *              "time": "2018-03-06T11:29:57.900Z"
 *          },
 *          {
 *              "id": {
 *                  "id": "2"
 *              },
 *              "comment": "Update 1 comment",
 *              "time": "2018-03-06T10:36:57.900Z"
 *          },
 *          {
 *              "id": {
 *                  "id": "1"
 *              },
 *              "comment": "Create Comment",
 *              "time": "2018-03-06T10:10:16.341Z"
 *          }
 *      ]
 *
 * }}}
 *
 * {{{curl 'http://localhost:4000/list'}}}
 *
 * Returns JSON listing the files in the config service repository.
 *
 * {{{
 *
 *      [
 *          {
 *              "path": "test3/TestConfig3",
 *              "id": {
 *                  "id": "13"
 *              },
 *              "comment": "Create comment"
 *          },
 *          {
 *              "path": "test2/TestConfig2",
 *              "id": {
 *                  "id": "3"
 *              },
 *              "comment": "Update 1 comment"
 *          },
 *          {
 *              "path": "test1/TestConfig1",
 *              "id": {
 *                  "id": "5"
 *              },
 *              "comment": "Update 3 comment"
 *          }
 *      ]
 *
 * }}}
 *
 * {{{curl 'http://localhost:4000/active-version/test1/TestConfig1}}}
 *
 * Returns the id of the active version of the file, which may or may not be the same as the id of latest version (see below).
 *
 * {{{curl -X PUT 'http://localhost:4000/active-version/test1/TestConfig1?id=5&comment=some+comment'}}}
 *
 * Sets the active version of the file to the one with the given id (an id returned by the history command).
 *
 * {{{curl -X PUT 'http://localhost:4000/active-version/test1/TestConfig1?comment=some+comment'}}}
 *
 * Resets the active version of the file to be the latest version.
 *
 * {{{curl 'http://localhost:4000/active-config/test1/TestConfig1'}}}
 *
 * Returns the content of the active version of the file, which may or may not be the same as the latest version (see below).
 *
 * {{{curl 'http://localhost:4000/metadata'}}}
 *
 * Returns JSON with config service metadata.
 * {{{
 *
 *      {
 *          "repoPath": "/tmp/csw-config-svn",
 *          "annexPath": "/tmp/csw-config-annex-files",
 *          "annexMinFileSize": "10 MiB",
 *          "maxConfigFileSize": "50 MiB"
 *      }
 *
 * }}}
 *
 * === Internal Impl Details ===
 *
 * Large/binary files can slow down the repository, so these are stored separately using
 * the the AnnexServiceRepo http file server.
 *
 * When you first create a config file, you can choose to store it in the normal way (in the repository)
 * or as a *large/binary* file, in which case only *file.\$sha1* is checked in, containing the SHA-1 hash of the
 * file's contents. The actual binary file is then stored on the annex server in a file name based on the SHA-1 hash.
 *
 * The config service also supports the concept of *active versions* of files. In this case a file named
 * *file.\$active* is checked in behind the scenes and contains the id of the active version of the file.
 *
 * The config service can be started as a standalone application. *sbt stage* installs the command under
 * target/universal/stage/bin.
 * The standalone configs service registers itself with the location service so that it
 * can be found by other applications.
 *
 * The contents of the files are exchanged using [Akka reactive streams](http://www.typesafe.com/activator/template/akka-stream-scala).
 *
 * Detailed documentation of Config Server application is available at:
 * https://tmtsoftware.github.io/csw/apps/cswonfigserverapp.html
 *
 * Detailed documentation of Config Service is available at:
 * https://tmtsoftware.github.io/csw/services/config.html
 */
package object config {}
