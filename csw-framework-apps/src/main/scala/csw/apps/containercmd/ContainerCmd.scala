package csw.apps.containercmd

import com.typesafe.config.ConfigFactory
import csw.apps.containercmd.cli.ArgsParser

class ContainerCmd {
  def run(args: Array[String], resources: Map[String, String] = Map.empty): Unit = {
    val choices: String = resources.keys.toList.filter(_.nonEmpty).mkString(", ")

    new ArgsParser().parse(args) match {
      case Some(options) =>
        if (options.local) {
          val config = ConfigFactory.parseFile(options.inputFilePath.get.toFile)
          Component.create(options.standalone, config)
        } else {
          //fetch from Config service
        }
    }
  }
}
