package csw.services.csclient.cli

import csw.services.config.api.models.{ConfigData, ConfigId}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime

class CommandLineRunner(configService: ConfigService, actorRuntime: ActorRuntime) {

  import Block.await
  import actorRuntime._

  def run(options: Options): Unit = {

    def create(): Unit = {
      val configData = ConfigData.fromPath(options.inputFilePath.get)
      val configId = await(configService.create(options.relativeRepoPath.get, configData, oversize = options.oversize,
          options.comment))
      println(s"File : ${options.relativeRepoPath.get} is created with id : ${configId.id}")
    }

    def update(): Unit = {
      val configData = ConfigData.fromPath(options.inputFilePath.get)
      val configId   = await(configService.update(options.relativeRepoPath.get, configData, options.comment))
      println(s"File : ${options.relativeRepoPath.get} is updated with id : ${configId.id}")
    }

    def get(): Unit = {
      val idOpt = options.id.map(ConfigId(_))

      val configDataOpt = (options.date, options.id) match {
        case (Some(date), _) ⇒ await(configService.getByTime(options.relativeRepoPath.get, date))
        case (_, Some(id))   ⇒ await(configService.getById(options.relativeRepoPath.get, ConfigId(id)))
        case (_, _)          ⇒ await(configService.getLatest(options.relativeRepoPath.get))
      }

      configDataOpt match {
        case Some(configData) ⇒
          val outputFile = await(configData.toPath(options.outputFilePath.get))
          println(s"Output file is created at location : ${outputFile.toAbsolutePath}")
        case None ⇒
      }
    }

    def exists(): Unit = {
      val exists = await(configService.exists(options.relativeRepoPath.get))
      println(s"File ${options.relativeRepoPath.get} exists in the repo? : $exists")
    }

    def delete(): Unit = {
      await(configService.delete(options.relativeRepoPath.get))
      println(s"File ${options.relativeRepoPath.get} deletion is completed.")
    }

    def list(): Unit = {
      val fileInfoes = await(configService.list())
      fileInfoes.foreach(i ⇒ println(s"${i.path}\t${i.id.id}\t${i.comment}"))
    }

    def history(): Unit = {
      val histList = await(configService.history(options.relativeRepoPath.get, options.maxFileVersions))
      histList.foreach(h => println(s"${h.id.id}\t${h.time}\t${h.comment}"))
    }

    def setDefault(): Unit = {
      val idOpt: Option[ConfigId] = options.id.map(ConfigId(_))
      await(configService.setDefault(options.relativeRepoPath.get, idOpt))
      println(s"${options.relativeRepoPath.get} file with id:${idOpt.getOrElse("latest")} is set as default")
    }

    def getDefault(): Unit = {
      val configDataOpt = await(configService.getDefault(options.relativeRepoPath.get))

      configDataOpt match {
        case Some(configData) ⇒
          val outputFile = await(configData.toPath(options.outputFilePath.get))
          println(
              s"Default version of repository file: ${options.relativeRepoPath.get} is saved at location: ${outputFile.toAbsolutePath}")
        case None ⇒
      }
    }

    options.op match {
      case "create"     => create()
      case "update"     => update()
      case "get"        => get()
      case "exists"     => exists()
      case "delete"     => delete()
      case "list"       => list()
      case "history"    => history()
      case "setDefault" => setDefault()
      case "getDefault" => getDefault()
      case x            => throw new RuntimeException(s"Unknown operation: $x")
    }
  }
}
