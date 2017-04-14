package csw.services.config.client.internal

import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{util, lang ⇒ jl}

import csw.services.config.api.javadsl.IConfigService
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.api.scaladsl.ConfigService

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._

class JConfigService(configService: ConfigService, actorRuntime: ActorRuntime) extends IConfigService {

  import actorRuntime._

  override def name: String =
    configService.name

  override def create(path: Path,
                      configData: ConfigData,
                      oversize: Boolean,
                      comment: String): CompletableFuture[ConfigId] =
    configService.create(path, configData, oversize, comment).toJava.toCompletableFuture

  override def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    create(path, configData, oversize = false, comment)

  override def create(path: Path, configData: ConfigData, oversize: Boolean): CompletableFuture[ConfigId] =
    create(path, configData, oversize, comment = "")

  override def create(path: Path, configData: ConfigData): CompletableFuture[ConfigId] =
    create(path, configData, oversize = false, comment = "")

  override def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    configService.update(path, configData, comment).toJava.toCompletableFuture

  override def update(path: Path, configData: ConfigData): CompletableFuture[ConfigId] =
    update(path, configData, comment = "")

  override def get(path: Path, id: Optional[ConfigId]): CompletableFuture[Optional[ConfigData]] =
    configService.get(path, id.asScala).map(_.asJava).toJava.toCompletableFuture

  override def get(path: Path): CompletableFuture[Optional[ConfigData]] =
    configService.get(path).map(_.asJava).toJava.toCompletableFuture

  override def get(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]] =
    configService.get(path, time).map(_.asJava).toJava.toCompletableFuture

  override def exists(path: Path): CompletableFuture[jl.Boolean] =
    configService.exists(path).toJava.toCompletableFuture.asInstanceOf[CompletableFuture[jl.Boolean]]

  override def delete(path: Path, comment: String): CompletableFuture[Unit] =
    configService.delete(path, comment).toJava.toCompletableFuture

  override def delete(path: Path): CompletableFuture[Unit] =
    delete(path, comment = "deleted")

  override def list(): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list().map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path, maxResults: Int): CompletableFuture[util.List[ConfigFileHistory]] =
    configService.history(path, maxResults).map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path): CompletableFuture[util.List[ConfigFileHistory]] =
    history(path, maxResults = Int.MaxValue)

  override def setDefault(path: Path, id: Optional[ConfigId]): CompletableFuture[Unit] =
    configService.setDefault(path, id.asScala).toJava.toCompletableFuture

  override def resetDefault(path: Path): CompletableFuture[Unit] =
    configService.resetDefault(path).toJava.toCompletableFuture

  override def getDefault(path: Path): CompletableFuture[Optional[ConfigData]] =
    configService.getDefault(path).map(_.asJava).toJava.toCompletableFuture

  override def asScala: ConfigService = configService
}
