package csw.services.config.server.internal

import java.io.File
import java.util
import java.util.{Date, Optional}
import java.util.concurrent.CompletionStage

import csw.services.config.api.javadsl.IConfigManager
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.api.scaladsl.ConfigManager

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._

class JConfigManager(configManager: ConfigManager) extends IConfigManager {

  import scala.concurrent.ExecutionContext.Implicits.global
  override def name: String =
    configManager.name

  override def create(path: File, configData: ConfigData, oversize: Boolean, comment: String): CompletionStage[ConfigId] =
    configManager.create(path, configData, oversize, comment).toJava

  override def update(path: File, configData: ConfigData, comment: String): CompletionStage[ConfigId] =
    configManager.update(path, configData, comment).toJava

  override def get(path: File, id: Optional[ConfigId]): CompletionStage[Optional[ConfigData]] =
    configManager.get(path, id.asScala).map(_.asJava).toJava

  override def get(path: File, date: Date): CompletionStage[Optional[ConfigData]] =
    configManager.get(path, date).map(_.asJava).toJava

  override def exists(path: File): CompletionStage[Boolean] =
    configManager.exists(path).toJava

  override def delete(path: File, comment: String): CompletionStage[Unit] =
    configManager.delete(path, comment).toJava

  override def list(): CompletionStage[util.List[ConfigFileInfo]] =
    configManager.list().map(_.asJava).toJava

  override def history(path: File, maxResults: Int): CompletionStage[util.List[ConfigFileHistory]] =
    configManager.history(path, maxResults).map(_.asJava).toJava

  override def setDefault(path: File, id: Optional[ConfigId]): CompletionStage[Unit] =
    configManager.setDefault(path, id.asScala).toJava

  override def resetDefault(path: File): CompletionStage[Unit] =
    configManager.resetDefault(path).toJava

  override def getDefault(path: File): CompletionStage[Optional[ConfigData]] =
    configManager.getDefault(path).map(_.asJava).toJava
}
