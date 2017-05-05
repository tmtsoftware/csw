package csw.services.csclient.cli

import csw.services.config.api.models.{ConfigData, ConfigId, FileType}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class CommandLineRunner(configService: ConfigService, actorRuntime: ActorRuntime) {

  import actorRuntime._

  def run(options: Options): Unit =
    options.op match {
      //adminApi
      case "create"             ⇒ create(options)
      case "update"             ⇒ update(options)
      case "get"                ⇒ get(options)
      case "delete"             ⇒ delete(options)
      case "list"               ⇒ list(options)
      case "history"            ⇒ history(options)
      case "setActiveVersion"   ⇒ setActiveVersion(options)
      case "resetActiveVersion" ⇒ resetActiveVersion(options)
      case "getActiveVersion"   ⇒ getActiveVersion(options)
      case "getActiveByTime"    ⇒ getActiveByTime(options)
      case "getMetadata"        ⇒ getMetadata(options)
      //clientApi
      case "exists"    ⇒ exists(options)
      case "getActive" ⇒ getActive(options)
      case x           ⇒ throw new RuntimeException(s"Unknown operation: $x")
    }

  //adminApi
  private def create(options: Options): Unit = {
    val configData = ConfigData.fromPath(options.inputFilePath.get)
    val configId =
      await(configService.create(options.relativeRepoPath.get, configData, annex = options.annex, options.comment))
    println(s"File : ${options.relativeRepoPath.get} is created with id : ${configId.id}")
  }

  private def update(options: Options): Unit = {
    val configData = ConfigData.fromPath(options.inputFilePath.get)
    val configId   = await(configService.update(options.relativeRepoPath.get, configData, options.comment))
    println(s"File : ${options.relativeRepoPath.get} is updated with id : ${configId.id}")
  }

  private def get(options: Options): Unit = {
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

  private def delete(options: Options): Unit = {
    await(configService.delete(options.relativeRepoPath.get))
    println(s"File ${options.relativeRepoPath.get} deletion is completed.")
  }

  private def list(options: Options): Unit = {
    val normal = options.normal
    val annex  = options.annex
    (annex, normal) match {
      case (true, true) ⇒ println("Please provide either normal or annex. See --help to know more.")
      case (true, _) ⇒
        val fileInfoes = await(configService.list(Some(FileType.Annex), options.pattern))
        fileInfoes.foreach(i ⇒ println(s"${i.path}\t${i.id.id}\t${i.comment}"))
      case (_, true) ⇒
        val fileInfoes = await(configService.list(Some(FileType.Normal), options.pattern))
        fileInfoes.foreach(i ⇒ println(s"${i.path}\t${i.id.id}\t${i.comment}"))
      case (_, _) ⇒
        val fileInfoes = await(configService.list(pattern = options.pattern))
        fileInfoes.foreach(i ⇒ println(s"${i.path}\t${i.id.id}\t${i.comment}"))
    }
  }

  private def history(options: Options): Unit = {
    val histList = await(configService.history(options.relativeRepoPath.get, options.maxFileVersions))
    histList.foreach(h => println(s"${h.id.id}\t${h.time}\t${h.comment}"))
  }

  private def setActiveVersion(options: Options): Unit = {
    val maybeConfigId = options.id.map(id ⇒ ConfigId(id))
    await(configService.setActiveVersion(options.relativeRepoPath.get, maybeConfigId.get, options.comment))
    println(s"${options.relativeRepoPath.get} file with id:${maybeConfigId.get.id} is set as default")
  }

  private def resetActiveVersion(options: Options): Unit = {
    await(configService.resetActiveVersion(options.relativeRepoPath.get, options.comment))
    println(s"${options.relativeRepoPath.get} file is reset to default")
  }

  private def getActiveVersion(options: Options): Unit = {
    val configId = await(configService.getActiveVersion(options.relativeRepoPath.get))
    println(s"${configId.id} is the active version of the file.")
  }

  private def getActiveByTime(options: Options): Unit = {
    val configDataOpt = await(configService.getActiveByTime(options.relativeRepoPath.get, options.date.get))

    configDataOpt match {
      case Some(configData) ⇒
        val outputFile = await(configData.toPath(options.outputFilePath.get))
        println(s"Output file is created at location : ${outputFile.toAbsolutePath}")
      case None ⇒
    }
  }

  private def getMetadata(options: Options): Unit = {
    val metaData = await(configService.getMetadata)
    println(metaData)
  }

  //clientApi
  private def exists(options: Options): Unit = {
    val exists = await(configService.exists(options.relativeRepoPath.get))
    println(s"File ${options.relativeRepoPath.get} exists in the repo? : $exists")
  }

  private def getActive(options: Options): Unit = {
    val configDataOpt = await(configService.getActive(options.relativeRepoPath.get))

    configDataOpt match {
      case Some(configData) ⇒
        val outputFile = await(configData.toPath(options.outputFilePath.get))
        println(s"Output file is created at location : ${outputFile.toAbsolutePath}")
      case None ⇒
    }
  }

  //command line app is by nature blocking.
  //Do not use such method in library/server side code
  private def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)
}
