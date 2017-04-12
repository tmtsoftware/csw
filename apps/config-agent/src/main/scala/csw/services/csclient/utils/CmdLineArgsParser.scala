package csw.services.csclient.utils

import java.io.File

import csw.services.csclient.models.Options
import scopt.OptionParser

/**
  * Parses the command line options using `scopt` library.
  */

object CmdLineArgsParser {

  val parser: OptionParser[Options] = new scopt.OptionParser[Options]("csClient") {
    head("csClient", System.getProperty("CSW_VERSION"))

    //create operation
    cmd("create") action { (_, c) =>
      c.copy(op = "create")
    } text "copies the input file in the repository at a specified path" children (

      arg[File]("<repositoryFilePath>") action { (x, c) =>
        c.copy(repositoryFilePath = Some(x))
      } text "path in the repository",

      opt[File]('i', "in") required () valueName "<inputFile>" action { (x, c) =>
        c.copy(inputFilePath = Some(x))
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

      arg[File]("<path>") action { (x, c) =>
        c.copy(repositoryFilePath = Some(x))
      } text "path in the repository",

      opt[File]('i', "in") required () valueName "<inputFile>" action { (x, c) =>
        c.copy(inputFilePath = Some(x))
      } text "input file path",

      opt[String]('c', "comment") action { (x, c) =>
        c.copy(comment = x)
      } text "optional create comment"
    )

    //get operation
    cmd("get") action { (_, c) =>
      c.copy(op = "get")
    } text "retrieves file with a given path from config service, and writes it to the output file" children (

      arg[File]("<repositoryFilePath>") action { (x, c) =>
        c.copy(repositoryFilePath = Some(x))
      } text "path of the file in the repository",

      opt[File]('o', "out") required () valueName "<outputFile>" action { (x, c) =>
        c.copy(outputFilePath = Some(x))
      } text "output file path",

      opt[String]("id") action { (x, c) =>
        c.copy(id = Some(x))
      } text "optional version id of the repository file to get"
    )

    //exists operation
    cmd("exists") action { (_, c) =>
      c.copy(op = "exists")
    } text "checks if the file exists at specified path in the repository" children (

      arg[File]("<repositoryFilePath>") action { (x, c) =>
        c.copy(repositoryFilePath = Some(x))
      } text "file path in the repository"
      )

    //delete operation
    cmd("delete") action { (_, c) =>
      c.copy(op = "delete")
    } text "deletes the file at specified path in the repository" children (

      arg[File]("<repositoryFilePath>") action { (x, c) =>
        c.copy(repositoryFilePath = Some(x))
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

      arg[File]("<path>") action { (x, c) =>
        c.copy(repositoryFilePath = Some(x))
      } text "file path in the repository"
      )

    //setDefault operation
    cmd("setDefault") action { (_, c) =>
      c.copy(op = "setDefault")
    } text "sets the default version of the file" children (

      arg[File]("<path>") action { (x, c) =>
        c.copy(repositoryFilePath = Some(x))
      } text "file path in the repository",

      opt[String]("id") action { (x, c) =>
        c.copy(id = Some(x))
      } text "optional version id to set as default for file"
    )

    //resetDefault operation

    //getDefault operation

    help("help")

    version("version")

    checkConfig { c =>
      if (c.op.isEmpty)
        failure("Please specify at least one command {get | create | update | exists | list | history | setDefault | getDefault | resetDefault}")
      else
        success
    }

    override def errorOnUnknownArgument: Boolean = false
  }
}