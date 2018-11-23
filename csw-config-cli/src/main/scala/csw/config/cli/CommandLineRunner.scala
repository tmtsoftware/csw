package csw.config.cli

import java.nio.file.{Files, Path}

import csw.auth.adapters.nativeapp.api.NativeAppAuthAdapter
import csw.config.api.exceptions.FileNotFound
import csw.config.api.models._
import csw.config.api.scaladsl.ConfigService
import csw.config.cli.args.Options
import csw.config.client.internal.ActorRuntime

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class CommandLineRunner(
    configService: ConfigService,
    actorRuntime: ActorRuntime,
    printLine: Any ⇒ Unit,
    nativeAuthAdapter: NativeAppAuthAdapter
) {

  import actorRuntime._

  def login(): Unit = {
    nativeAuthAdapter.login()
    printLine(s"SUCCESS : Logged in successfully")
  }
  def logout(): Unit = {
    nativeAuthAdapter.logout()
    printLine(s"SUCCESS : Logged out successfully")
  }

  //adminApi
  def create(options: Options): ConfigId = {
    val inputFilePath = options.inputFilePath.get
    if (Files.exists(inputFilePath)) {
      val configData = ConfigData.fromPath(inputFilePath)
      val configId =
        await(
          configService.create(options.relativeRepoPath.get, configData, annex = options.annex, options.comment.get)
        )
      printLine(s"File : ${options.relativeRepoPath.get} is created with id : ${configId.id}")
      configId
    } else throw FileNotFound(inputFilePath)
  }

  def update(options: Options): ConfigId = {
    val inputFilePath = options.inputFilePath.get
    if (Files.exists(inputFilePath)) {
      val configData = ConfigData.fromPath(inputFilePath)
      val configId   = await(configService.update(options.relativeRepoPath.get, configData, options.comment.get))
      printLine(s"File : ${options.relativeRepoPath.get} is updated with id : ${configId.id}")
      configId
    } else throw FileNotFound(inputFilePath)
  }

  def get(options: Options): Option[Path] = {
    val configDataOpt = (options.date, options.id) match {
      case (Some(date), _) ⇒ await(configService.getByTime(options.relativeRepoPath.get, date))
      case (_, Some(id))   ⇒ await(configService.getById(options.relativeRepoPath.get, ConfigId(id)))
      case (_, _)          ⇒ await(configService.getLatest(options.relativeRepoPath.get))
    }

    configDataOpt match {
      case Some(configData) ⇒
        val outputFile = await(configData.toPath(options.outputFilePath.get))
        printLine(s"Output file is created at location : ${outputFile.toAbsolutePath}")
        Some(outputFile.toAbsolutePath)
      case None ⇒
        printLine(FileNotFound(options.relativeRepoPath.get).message)
        None
    }
  }

  def delete(options: Options): Unit = {
    await(configService.delete(options.relativeRepoPath.get, "no longer needed"))
    printLine(s"File ${options.relativeRepoPath.get} deletion is completed.")
  }

  def list(options: Options): List[ConfigFileInfo] = {
    val maybeList: Option[List[ConfigFileInfo]] = (options.annex, options.normal) match {
      case (true, true) ⇒ None
      case (true, _)    ⇒ Some(await(configService.list(Some(FileType.Annex), options.pattern)))
      case (_, true)    ⇒ Some(await(configService.list(Some(FileType.Normal), options.pattern)))
      case (_, _)       ⇒ Some(await(configService.list(pattern = options.pattern)))
    }

    maybeList match {
      case None                   ⇒ printLine("Please provide either normal or annex. See --help to know more.")
      case Some(xs) if xs.isEmpty ⇒ printLine("List returned empty results.")
      case Some(xs)               ⇒ xs.foreach(i ⇒ printLine(s"${i.path}\t${i.id.id}\t${i.comment}"))
    }

    maybeList.getOrElse(List.empty)
  }

  def history(options: Options): List[ConfigFileRevision] = {
    val fileRevisions = await(
      configService.history(options.relativeRepoPath.get, options.fromDate, options.toDate, options.maxFileVersions)
    )
    fileRevisions.foreach(h => printLine(s"${h.id.id}\t${h.time}\t${h.comment}"))
    fileRevisions
  }

  def historyActive(options: Options): List[ConfigFileRevision] = {
    val fileRevisions = await(
      configService
        .historyActive(options.relativeRepoPath.get, options.fromDate, options.toDate, options.maxFileVersions)
    )
    fileRevisions.foreach(h => printLine(s"${h.id.id}\t${h.time}\t${h.comment}"))
    fileRevisions
  }

  def setActiveVersion(options: Options): Unit = {
    val maybeConfigId = options.id.map(id ⇒ ConfigId(id))
    await(configService.setActiveVersion(options.relativeRepoPath.get, maybeConfigId.get, options.comment.get))
    printLine(s"${options.relativeRepoPath.get} file with id:${maybeConfigId.get.id} is set as active")
  }

  def resetActiveVersion(options: Options): Unit = {
    await(configService.resetActiveVersion(options.relativeRepoPath.get, options.comment.get))
    printLine(s"${options.relativeRepoPath.get} file is reset to active")
  }

  def getActiveVersion(options: Options): Option[ConfigId] = {
    val maybeId = await(configService.getActiveVersion(options.relativeRepoPath.get))

    maybeId match {
      case Some(configId) ⇒ printLine(s"Id : ${configId.id} is the active version of the file.")
      case None           ⇒ printLine(FileNotFound(options.relativeRepoPath.get).message)
    }

    maybeId
  }

  def getActiveByTime(options: Options): Option[Path] = {
    val maybeConfigData = await(configService.getActiveByTime(options.relativeRepoPath.get, options.date.get))

    maybeConfigData match {
      case Some(configData) ⇒
        val outputFile = await(configData.toPath(options.outputFilePath.get))
        printLine(s"Output file is created at location : ${outputFile.toAbsolutePath}")
        Some(outputFile.toAbsolutePath)
      case None ⇒
        printLine(FileNotFound(options.relativeRepoPath.get).message)
        None
    }
  }

  def getMetadata(options: Options): ConfigMetadata = {
    val metaData = await(configService.getMetadata)
    printLine(metaData.toString)
    metaData
  }

  //clientApi
  def exists(options: Options): Boolean = {
    val exists = await(configService.exists(options.relativeRepoPath.get))
    printLine(s"File ${options.relativeRepoPath.get} exists in the repo? : $exists")
    exists
  }

  def getActive(options: Options): Option[Path] = {
    val maybeConfigData = await(configService.getActive(options.relativeRepoPath.get))

    maybeConfigData match {
      case Some(configData) ⇒
        val outputFile = await(configData.toPath(options.outputFilePath.get))
        printLine(s"Output file is created at location : ${outputFile.toAbsolutePath}")
        Some(outputFile.toAbsolutePath)
      case None ⇒
        printLine(FileNotFound(options.relativeRepoPath.get).message)
        None
    }
  }

  // command line app is by nature blocking.
  // Do not use such method in library/server side code
  private def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)
}
