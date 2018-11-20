package csw.config.cli.args

import java.nio.file.Paths
import java.time.Instant

import csw.services.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    //login operation
    cmd("login") action { (_, c) =>
      c.copy(op = "login")
    } text "login to access admin API's"

    //logout operation
    cmd("logout") action { (_, c) =>
      c.copy(op = "logout")
    } text "logout"

    //create operation
    cmd("create") action { (_, c) =>
      c.copy(op = "create")
    } text "creates the input file in the repository at a specified path" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "path in the repository",
      opt[String]('i', "in") required () valueName "<inputFile>" action { (x, c) =>
        c.copy(inputFilePath = Some(Paths.get(x)))
      } text "input file path",
      opt[Unit]("annex") action { (_, c) =>
        c.copy(annex = true)
      } text "optional add this option for large/binary files",
      opt[String]('c', "comment") required () action { (x, c) =>
        c.copy(comment = Some(x))
      } text "create comment"
    )

    //update operation
    cmd("update") action { (_, c) =>
      c.copy(op = "update")
    } text "overwrites the file specified in the repository by the input file" children (
      arg[String]("<path>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "path in the repository",
      opt[String]('i', "in") required () valueName "<inputFile>" action { (x, c) =>
        c.copy(inputFilePath = Some(Paths.get(x)))
      } text "input file path",
      opt[String]('c', "comment") required () action { (x, c) =>
        c.copy(comment = Some(x))
      } text "create comment"
    )

    //get operation
    cmd("get") action { (_, c) =>
      c.copy(op = "get")
    } text "retrieves a file for a given path and saves it to the output file. Latest file is fetched if neither date nor id is specified." children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "path of the file in the repository",
      opt[String]('o', "out") required () valueName "<outputFile>" action { (x, c) =>
        c.copy(outputFilePath = Some(Paths.get(x)))
      } text "output file path",
      opt[String]("id") action { (x, c) =>
        c.copy(id = Some(x))
      } text "optional. if specified this id will be matched",
      opt[String]("date") action { (x, c) =>
        c.copy(date = Some(Instant.parse(x)))
      } text "optional. if specified will get the file matching this date. Format: 2017-04-16T16:15:23.503Z"
    )

    //delete operation
    cmd("delete") action { (_, c) =>
      c.copy(op = "delete")
    } text "deletes the file at specified path in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[String]('c', "comment") required () action { (x, c) =>
        c.copy(comment = Some(x))
      } text "delete comment"
    )

    //list operation
    cmd("list") action { (_, c) =>
      c.copy(op = "list")
    } text "lists the files in the repository" children (
      opt[Unit]("annex") action { (_, c) =>
        c.copy(annex = true)
      } text "optional add this option to list only large/binary files",
      opt[Unit]("normal") action { (_, c) =>
        c.copy(normal = true)
      } text "optional add this option to list only non-binary/large files",
      opt[String]("pattern") action { (x, c) =>
        c.copy(pattern = Some(x))
      } text "optional list all files whose path matches the given pattern"
    )

    //history operation
    cmd("history") action { (_, c) =>
      c.copy(op = "history")
    } text "shows versioning history of the file in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[String]("from") action { (x, c) =>
        c.copy(fromDate = Instant.parse(x))
      } text "optional date parameter for start date ex. 2017-04-16T16:15:23.503Z",
      opt[String]("to") action { (x, c) =>
        c.copy(toDate = Instant.parse(x))
      } text "optional date parameter for upto date ex. 2017-04-16T16:15:23.503Z",
      opt[Int]("max") action { (x, c) =>
        c.copy(maxFileVersions = x)
      } text "optional maximum entries of file versions"
    )

    //setActiveVersion operation
    cmd("setActiveVersion") action { (_, c) =>
      c.copy(op = "setActiveVersion")
    } text "sets active version of the file in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[String]("id") required () action { (x, c) =>
        c.copy(id = Some(x))
      } text "version id of file to be set as active",
      opt[String]('c', "comment") required () action { (x, c) =>
        c.copy(comment = Some(x))
      } text "create comment"
    )

    //resetActiveVersion operation
    cmd("resetActiveVersion") action { (_, c) =>
      c.copy(op = "resetActiveVersion")
    } text "resets the active version to the latest version for the specified file" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[String]('c', "comment") required () action { (x, c) =>
        c.copy(comment = Some(x))
      } text "reset comment"
    )

    //getActiveVersion operation
    cmd("getActiveVersion") action { (_, c) =>
      c.copy(op = "getActiveVersion")
    } text "gets the id of the active version of the file in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository"
    )

    //getActiveByTime operation
    cmd("getActiveByTime") action { (_, c) =>
      c.copy(op = "getActiveByTime")
    } text "gets the file that was active at a specified time" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[String]("date") required () action { (x, c) =>
        c.copy(date = Some(Instant.parse(x)))
      } text "optional. if specified will get the active file matching this date. Format: 2017-04-16T16:15:23.503Z",
      opt[String]('o', "out") required () valueName "<outputFile>" action { (x, c) =>
        c.copy(outputFilePath = Some(Paths.get(x)))
      } text "output file path"
    )

    //history operation
    cmd("historyActive") action { (_, c) =>
      c.copy(op = "historyActive")
    } text "shows versioning history of the active file in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[String]("from") action { (x, c) =>
        c.copy(fromDate = Instant.parse(x))
      } text "optional date parameter for start date ex. 2017-04-16T16:15:23.503Z",
      opt[String]("to") action { (x, c) =>
        c.copy(toDate = Instant.parse(x))
      } text "optional date parameter for upto date ex. 2017-04-16T16:15:23.503Z",
      opt[Int]("max") action { (x, c) =>
        c.copy(maxFileVersions = x)
      } text "optional maximum entries of file versions"
    )

    //getMetadata operation
    cmd("getMetadata") action { (_, c) =>
      c.copy(op = "getMetadata")
    } text "gets the metadata of config server e.g. repository directory, annex directory, min annex file size, max config file size"

    //exists operation
    cmd("exists") action { (_, c) =>
      c.copy(op = "exists")
    } text "checks if the file exists at specified path in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository"
    )

    //getActive operation
    cmd("getActive") action { (_, c) =>
      c.copy(op = "getActive")
    } text "retrieves active file for a given path from config service, and writes it to the output file" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "path of the file in the repository",
      opt[String]('o', "out") required () valueName "<outputFile>" action { (x, c) =>
        c.copy(outputFilePath = Some(Paths.get(x)))
      } text "output file path"
    )

    opt[String]("locationHost") action { (x, c) =>
      c.copy(locationHost = x)
    } text "Optional: host address of machine where location server is running. Default is localhost"

    help("help")

    version("version")

    checkConfig { c =>
      if (c.op.isEmpty)
        failure(
          """
            |Please specify one of the following command with their corresponding options:
            | - create
            | - update
            | - get
            | - delete
            | - list
            | - history
            | - setActiveVersion
            | - resetActiveVersion
            | - getActiveVersion
            | - getActiveByTime
            | - getMetadata
            | - exists
            | - getActive
          """.stripMargin
        )
      else
        success
    }
  }

  /**
   * Parses the command line arguments and returns a value if they are valid.
   *
   * @param args the command line arguments
   * @return an object containing the parsed values of the command line arguments
   */
  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())

}
