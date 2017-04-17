package csw.services.csclient

import akka.Done
import akka.actor.ActorSystem
import csw.services.config.api.models.ConfigId
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.csclient.models.Options
import csw.services.csclient.utils.{CmdLineArgsParser, PathUtils}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationServiceFactory

import scala.async.Async._
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

class ConfigCliApp(clusterSettings: ClusterSettings) {

  val actorRuntime = new ActorRuntime(ActorSystem())

  import actorRuntime._

  private val locationService = LocationServiceFactory.withSettings(clusterSettings)
  private val configService: ConfigService = ConfigClientFactory.make(actorSystem, locationService)

  def start(args: Array[String]): Future[Unit] = async {
    CmdLineArgsParser.parse(args) match {
      case Some(options) =>
        await(commandLineRunner(options))
        await(shutdown())
        System.exit(0)
      case None          =>
        System.exit(1)
    }
  } recoverWith {
    case NonFatal(ex) ⇒
      ex.printStackTrace(System.err)
      async {
        await(shutdown())
        System.exit(1)
      }
  }

  def shutdown(): Future[Done] = async {
    await(actorSystem.terminate())
    await(locationService.shutdown())
  }

  def commandLineRunner(options: Options): Future[Unit] = {

    def create(): Future[Unit] = async {
      val configData = PathUtils.fromPath(options.inputFilePath.get)
      val configId = await(configService.create(options.repositoryFilePath.get, configData, oversize = options.oversize, options.comment))
      println(s"File : ${options.repositoryFilePath.get} is created with id : ${configId.id}")
    }

    def update() = async {
      val configData = PathUtils.fromPath(options.inputFilePath.get)
      val configId = await(configService.update(options.repositoryFilePath.get, configData, options.comment))
      println(s"File : ${options.repositoryFilePath.get} is updated with id : ${configId.id}")
    }

    def get(): Future[Unit] = async {
      val idOpt = options.id.map(ConfigId(_))

      val configDataOpt = options.date match {
        case Some(date) ⇒ await(configService.get(options.repositoryFilePath.get, date))
        case None       ⇒ await(configService.get(options.repositoryFilePath.get, idOpt))
      }

      configDataOpt match {
        case Some(configData) ⇒
          val outputFile = await(PathUtils.writeToPath(configData, options.outputFilePath.get))
          println(s"Output file is created at location : ${outputFile.getAbsolutePath}")
        case None             ⇒
      }
    }

    def exists(): Future[Unit] = async {
      val exists = await(configService.exists(options.repositoryFilePath.get))
      println(s"File ${options.repositoryFilePath.get} exists in the repo? : $exists")
    }

    def delete(): Future[Unit] = async {
      await(configService.delete(options.repositoryFilePath.get))
      println(s"File ${options.repositoryFilePath.get} deletion is completed.")
    }

    def list(): Future[Unit] = async {
      val fileInfoes = await(configService.list())
      fileInfoes.foreach(i ⇒ println(s"${i.path}\t${i.id.id}\t${i.comment}"))
    }

    def history(): Future[Unit] = async {
      val histList = await(configService.history(options.repositoryFilePath.get, options.maxFileVersions))
      histList.foreach(h => println(s"$h.id.id\t$h.time\t$h.comment"))
    }

    def setDefault(): Future[Unit] = async {
      val idOpt: Option[ConfigId] = options.id.map(ConfigId(_))
      await(configService.setDefault(options.repositoryFilePath.get, idOpt))
      println(s"${options.repositoryFilePath.get} file with id:${idOpt.getOrElse("latest")} is set as default")
    }

    def getDefault: Future[Unit] = async {
      val configDataOpt = await(configService.getDefault(options.repositoryFilePath.get))

      configDataOpt match {
        case Some(configData) ⇒
          val outputFile = await(PathUtils.writeToPath(configDataOpt.get, options.outputFilePath.get))
          println(s"Default version of repository file: ${options.repositoryFilePath.get} is saved at location: ${outputFile.getAbsolutePath}")
        case None             ⇒
      }
    }

    options.op match {
      case "create"       => create()
      case "update"       => update()
      case "get"          => get()
      case "exists"       => exists()
      case "delete"       => delete()
      case "list"         => list()
      case "history"      => history()
      case "setDefault"   => setDefault()
      case "getDefault"   => getDefault
      case x              => throw new RuntimeException(s"Unknown operation: $x")
    }
  }
}

object ConfigCliApp {

  def main(args: Array[String]): Unit = {
    Await.result(new ConfigCliApp(ClusterSettings()).start(args), 5.seconds)
  }

}
