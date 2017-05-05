package csw.services.config.client.internal

import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{util, lang ⇒ jl}

import csw.services.config.api.commons.FileType
import csw.services.config.api.javadsl.IConfigService
import csw.services.config.api.models.{ConfigData, ConfigFileInfo, ConfigFileRevision, ConfigId}
import csw.services.config.api.scaladsl.ConfigService

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._

class JConfigService(configService: ConfigService, actorRuntime: ActorRuntime) extends IConfigService {

  import actorRuntime._

  override def create(path: Path,
                      configData: ConfigData,
                      annex: Boolean,
                      comment: String): CompletableFuture[ConfigId] =
    configService.create(path, configData, annex, comment).toJava.toCompletableFuture

  override def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    create(path, configData, annex = false, comment)

  override def create(path: Path, configData: ConfigData, annex: Boolean): CompletableFuture[ConfigId] =
    create(path, configData, annex, comment = "")

  override def create(path: Path, configData: ConfigData): CompletableFuture[ConfigId] =
    create(path, configData, annex = false, comment = "")

  override def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    configService.update(path, configData, comment).toJava.toCompletableFuture

  override def update(path: Path, configData: ConfigData): CompletableFuture[ConfigId] =
    update(path, configData, comment = "")

  override def getById(path: Path, id: ConfigId): CompletableFuture[Optional[ConfigData]] =
    configService.getById(path, id).map(_.asJava).toJava.toCompletableFuture

  override def getLatest(path: Path): CompletableFuture[Optional[ConfigData]] =
    configService.getLatest(path).map(_.asJava).toJava.toCompletableFuture

  override def getByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]] =
    configService.getByTime(path, time).map(_.asJava).toJava.toCompletableFuture

  override def exists(path: Path): CompletableFuture[jl.Boolean] =
    configService.exists(path).toJava.toCompletableFuture.asInstanceOf[CompletableFuture[jl.Boolean]]

  override def exists(path: Path, id: Optional[ConfigId]): CompletableFuture[jl.Boolean] =
    configService.exists(path, id.asScala).toJava.toCompletableFuture.asInstanceOf[CompletableFuture[jl.Boolean]]

  override def delete(path: Path, comment: String): CompletableFuture[Unit] =
    configService.delete(path, comment).toJava.toCompletableFuture

  override def delete(path: Path): CompletableFuture[Unit] =
    delete(path, comment = "deleted")

  override def list(fileType: FileType, pattern: String): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(Some(fileType), Some(pattern)).map(_.asJava).toJava.toCompletableFuture

  override def list(fileType: FileType): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(Some(fileType)).map(_.asJava).toJava.toCompletableFuture

  override def list(pattern: String): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(pattern = Some(pattern)).map(_.asJava).toJava.toCompletableFuture

  override def list(): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list().map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    configService.history(path, maxResults).map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, maxResults = Int.MaxValue)

  override def setActive(path: Path, id: ConfigId, comment: String): CompletableFuture[Unit] =
    configService.setActive(path, id, comment).toJava.toCompletableFuture

  override def setActive(path: Path, id: ConfigId): CompletableFuture[Unit] =
    setActive(path, id, "")

  override def resetActive(path: Path, comment: String): CompletableFuture[Unit] =
    configService.resetActive(path, comment).toJava.toCompletableFuture

  override def resetActive(path: Path): CompletableFuture[Unit] =
    resetActive(path, "")

  override def getActive(path: Path): CompletableFuture[Optional[ConfigData]] =
    configService.getActive(path).map(_.asJava).toJava.toCompletableFuture

  override def asScala: ConfigService = configService

  override def getActiveVersion(path: Path): CompletableFuture[ConfigId] =
    configService.getActiveVersion(path).toJava.toCompletableFuture

  override def getActiveByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]] =
    configService.getActiveByTime(path, time).map(_.asJava).toJava.toCompletableFuture
}
