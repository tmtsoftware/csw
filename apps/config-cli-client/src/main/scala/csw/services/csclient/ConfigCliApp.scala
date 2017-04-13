package csw.services.csclient

import java.io.File

import csw.services.config.api.models.{ConfigData, ConfigId}
import csw.services.config.client.internal.ClientWiring
import csw.services.csclient.models.Options
import csw.services.csclient.utils.CmdLineArgsParser

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ConfigCliApp {

  private lazy val clientWiring = new ClientWiring
  import clientWiring._
  import actorRuntime._

  def start(args: Array[String]): Any = CmdLineArgsParser.parse(args) match {
    case Some(options) => commandLineRunner(options).onComplete {
        case Success(_) =>
          locationService.shutdown().onComplete(_ => System.exit(0))
        case Failure(ex) =>
          System.err.println(s"Error: ${ex.getMessage}")
          ex.printStackTrace(System.err)
          locationService.shutdown().onComplete(_ => System.exit(1))
      }
    case None => System.exit(1)
  }

  private def commandLineRunner(options: Options): Future[Unit] = {

    def get(): Future[Unit] = {
      val idOpt: Option[ConfigId] = options.id.map(ConfigId(_))

      for {
        configDataOpt: Option[ConfigData] <- configService.get(options.repositoryFilePath.get, idOpt)
        if configDataOpt.isDefined
        outputFile: File <- configDataOpt.get.toFileF(options.outputFilePath.get)
      } yield {
        println(s"Output file is created at location : ${outputFile.getAbsolutePath}")
      }

    }

    def create(): Future[Unit] = {
      val configData: ConfigData = ConfigData.fromPath(options.inputFilePath.get)
      for {
        configId <- configService.create(options.repositoryFilePath.get, configData, oversize = options.oversize,
          options.comment)
      } yield {
        println(s"File : ${options.repositoryFilePath.get} is created with id : ${configId.id}")
      }
    }

    options.op match {
      case "get" => get()
      case "create" => create()
      case x =>
        throw new RuntimeException(s"Unknown operation: $x")
    }

  }
}

object ConfigCliApp extends App {
  new ConfigCliApp().start(args)
}
