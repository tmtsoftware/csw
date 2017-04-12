package csw.services.config.server.internal

import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{util, lang => jl}

import csw.services.config.api.javadsl.IConfigManager
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.api.scaladsl.ConfigManager
import csw.services.config.server.ActorRuntime

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._

class JConfigManager(configManager: ConfigManager, actorRuntime: ActorRuntime) extends IConfigManager {

  import actorRuntime._

  override def name: String =
    configManager.name

  override def create(path: Path,
                      configData: ConfigData,
                      oversize: Boolean,
                      comment: String): CompletableFuture[ConfigId] =
    configManager.create(path, configData, oversize, comment).toJava.toCompletableFuture

  override def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    create(path, configData, oversize = false, comment)

  override def create(path: Path, configData: ConfigData, oversize: Boolean): CompletableFuture[ConfigId] =
    create(path, configData, oversize, comment = "")

  override def create(path: Path, configData: ConfigData): CompletableFuture[ConfigId] =
    create(path, configData, oversize = false, comment = "")

  override def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    configManager.update(path, configData, comment).toJava.toCompletableFuture

  override def update(path: Path, configData: ConfigData): CompletableFuture[ConfigId] =
    update(path, configData, comment = "")

  override def get(path: Path, id: Optional[ConfigId]): CompletableFuture[Optional[ConfigData]] =
    configManager.get(path, id.asScala).map(_.asJava).toJava.toCompletableFuture

  override def get(path: Path): CompletableFuture[Optional[ConfigData]] =
    configManager.get(path).map(_.asJava).toJava.toCompletableFuture

  override def get(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]] =
    configManager.get(path, time).map(_.asJava).toJava.toCompletableFuture

  override def exists(path: Path): CompletableFuture[jl.Boolean] =
    configManager.exists(path).toJava.toCompletableFuture.asInstanceOf[CompletableFuture[jl.Boolean]]

  override def delete(path: Path, comment: String): CompletableFuture[Unit] =
    configManager.delete(path, comment).toJava.toCompletableFuture

  override def delete(path: Path): CompletableFuture[Unit] =
    delete(path, comment = "deleted")

  override def list(): CompletableFuture[util.List[ConfigFileInfo]] =
    configManager.list().map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path, maxResults: Int): CompletableFuture[util.List[ConfigFileHistory]] =
    configManager.history(path, maxResults).map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path): CompletableFuture[util.List[ConfigFileHistory]] =
    history(path, maxResults = Int.MaxValue)

  override def setDefault(path: Path, id: Optional[ConfigId]): CompletableFuture[Unit] =
    configManager.setDefault(path, id.asScala).toJava.toCompletableFuture

  override def resetDefault(path: Path): CompletableFuture[Unit] =
    configManager.resetDefault(path).toJava.toCompletableFuture

  override def getDefault(path: Path): CompletableFuture[Optional[ConfigData]] =
    configManager.getDefault(path).map(_.asJava).toJava.toCompletableFuture
}
