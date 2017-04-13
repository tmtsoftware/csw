package csw.services.csclient

import java.io.File

import akka.Done
import csw.services.config.api.models.{ConfigData, ConfigId}
import csw.services.config.client.internal.ClientWiring
import csw.services.csclient.models.Options
import csw.services.csclient.utils.{CmdLineArgsParser, PathUtils}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ConfigCliApp extends App {

  private lazy val clientWiring = new ClientWiring
  import clientWiring._
  import actorRuntime._

  CmdLineArgsParser.parse(args) match {
    case Some(options) =>
      commandLineRunner(options).onComplete {
        case Success(_) => shutdown.onComplete(_ => System.exit(0))
        case Failure(ex) =>
          System.err.println(s"Error: ${ex.getMessage}")
          ex.printStackTrace(System.err)
          shutdown.onComplete(_ => System.exit(1))
      }
    case None => System.exit(1)
  }

  def shutdown(): Future[Done] ={
    clientWiring.actorSystem.terminate()
    locationService.shutdown()
  }

  def commandLineRunner(options: Options): Future[Unit] = {

    def create(): Future[Unit] = {
      val configData: ConfigData = PathUtils.fromPath(options.inputFilePath.get)
      for {
        configId <- configService.create(options.repositoryFilePath.get, configData, oversize = options.oversize,
          options.comment)
      } yield {
        println(s"File : ${options.repositoryFilePath.get} is created with id : ${configId.id}")
      }
    }

    def update() = {
      val configData: ConfigData = PathUtils.fromPath(options.inputFilePath.get)

      for {
        configId <- configService.update(options.repositoryFilePath.get, configData, options.comment)
      } yield {
        println(s"File : ${options.repositoryFilePath.get} is updated with id : ${configId.id}")
      }
    }

    def get(): Future[Unit] = {
      val idOpt: Option[ConfigId] = options.id.map(ConfigId(_))

      for {
        configDataOpt: Option[ConfigData] <- configService.get(options.repositoryFilePath.get, idOpt)
        if configDataOpt.isDefined
          outputFile: File <- PathUtils.writeToPath(configDataOpt.get, options.outputFilePath.get)
      } yield {
        println(s"Output file is created at location : ${outputFile.getAbsolutePath}")
      }
    }

    def exists(): Future[Unit] = {
      configService.exists(options.repositoryFilePath.get).map { bExists =>
        println(s"File ${options.repositoryFilePath.get} exists in the repo? : $bExists")
      }
    }

    def delete(): Future[Unit] = {
      configService.delete(options.repositoryFilePath.get).map { _ =>
        println(s"File ${options.repositoryFilePath.get} deletion is completed.")
      }
    }

    def list(): Future[Unit] = {
      for {
        infoList <- configService.list()
      } yield {
        for (i <- infoList) {
          println(s"${i.path}\t${i.id.id}\t${i.comment}")
        }
      }
    }

    def history(): Future[Unit] = {
      for {
        histList <- configService.history(options.repositoryFilePath.get, options.maxFileVersions)
      } yield {
        for (h <- histList) {
          println(s"${h.id.id}\t${h.time}\t${h.comment}")
        }
      }
    }

    def setDefault(): Future[Unit] = {
      val idOpt: Option[ConfigId] = options.id.map(ConfigId(_))
      configService.setDefault(options.repositoryFilePath.get, idOpt).map{ _ =>
        println(s"${options.repositoryFilePath.get} file with id:${idOpt.getOrElse("latest")} is set as default")
      }
    }

    def getDefault: Future[Unit] = {
      for {
        configDataOpt <- configService.getDefault(options.repositoryFilePath.get)
        if configDataOpt.isDefined
          outputFile <- PathUtils.writeToPath(configDataOpt.get, options.outputFilePath.get)
      } yield {
        println(s"Default version of repository file: ${options.repositoryFilePath.get} is saved at location: ${outputFile.getAbsolutePath}")
      }
    }

    def resetDefault(): Future[Unit] = {
      configService.resetDefault(options.repositoryFilePath.get).map { _ =>
        println(s"Default version of file ${options.repositoryFilePath.get} is set to latest.")
      }
    }

    options.op match {
      case "create" => create()
      case "update" => update()
      case "get" => get()
      case "exists" => exists()
      case "delete" => delete()
      case "list" => list()
      case "history"        => history()
      case "setDefault"     => setDefault()
      case "getDefault"     => getDefault
      case "resetDefault"   => resetDefault()
      case x =>
        throw new RuntimeException(s"Unknown operation: $x")
    }

  }

}
