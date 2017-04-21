package csw.services.csclient.utils

import java.nio.file.Paths
import java.time.Instant

import csw.services.BuildInfo
import csw.services.csclient.models.Options
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
object ArgsParser {

  val parser: OptionParser[Options] = new scopt.OptionParser[Options]("csw-config-client-cli") {
    head(BuildInfo.name, BuildInfo.version)

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
      opt[Unit]("oversize") action { (_, c) =>
        c.copy(oversize = true)
      } text "optional add this option for large/binary files",
      opt[String]('c', "comment") action { (x, c) =>
        c.copy(comment = x)
      } text "optional create comment"
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
      opt[String]('c', "comment") action { (x, c) =>
        c.copy(comment = x)
      } text "optional create comment"
    )

    //get operation
    cmd("get") action { (_, c) =>
      c.copy(op = "get")
    } text "retrieves file with a given path from config service, and writes it to the output file" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "path of the file in the repository",
      opt[String]('o', "out") required () valueName "<outputFile>" action { (x, c) =>
        c.copy(outputFilePath = Some(Paths.get(x)))
      } text "output file path",
      opt[String]("id") action { (x, c) =>
        c.copy(id = Some(x))
      } text "optional version id of the repository file to get",
      opt[String]("date") action { (x, c) =>
        c.copy(date = Some(Instant.parse(x)))
      } text "optional date parameter ex. 2017-04-16T16:15:23.503Z"
    )

    //exists operation
    cmd("exists") action { (_, c) =>
      c.copy(op = "exists")
    } text "checks if the file exists at specified path in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository"
    )

    //delete operation
    cmd("delete") action { (_, c) =>
      c.copy(op = "delete")
    } text "deletes the file at specified path in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository"
    )

    //list operation
    cmd("list") action { (_, c) =>
      c.copy(op = "list")
    } text "lists the files in the repository" children ()

    //history operation
    cmd("history") action { (_, c) =>
      c.copy(op = "history")
    } text "shows versioning history of the file in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[Int]("max") action { (x, c) =>
        c.copy(maxFileVersions = x)
      } text "optional maximum entries of file versions"
    )

    //setDefault operation
    cmd("setDefault") action { (_, c) =>
      c.copy(op = "setDefault")
    } text "sets default version of the file in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[String]("id") action { (x, c) =>
        c.copy(id = Some(x))
      } text "optional version id of file to be set as default"
    )

    //resetDefault operation
    cmd("resetDefault") action { (_, c) =>
      c.copy(op = "resetDefault")
    } text "resets the default to the latest version of the file in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository"
    )

    //getDefault operation
    cmd("getDefault") action { (_, c) =>
      c.copy(op = "getDefault")
    } text "gets the default version of the file in the repository" children (
      arg[String]("<relativeRepoPath>") action { (x, c) =>
        c.copy(relativeRepoPath = Some(Paths.get(x)))
      } text "file path in the repository",
      opt[String]('o', "out") required () valueName "<outputFile>" action { (x, c) =>
        c.copy(outputFilePath = Some(Paths.get(x)))
      } text "output file"
    )

    help("help")

    version("version")

    checkConfig { c =>
      if (c.op.isEmpty)
        failure(
            "Please specify at least one command {get | create | update | exists | list | history | setDefault | getDefault | resetDefault}")
      else
        success
    }

    override def errorOnUnknownArgument = false

  }

  /**
   * Parses the command line arguments and returns a value if they are valid.
   *
   * @param args the command line arguments
   * @return an object containing the parsed values of the command line arguments
   */
  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())

}
