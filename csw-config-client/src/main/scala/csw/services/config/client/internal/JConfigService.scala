package csw.services.config.client.internal

import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{util, lang ⇒ jl}

import csw.services.config.api.javadsl.IConfigService
import csw.services.config.api.models._
import csw.services.config.api.scaladsl.ConfigService

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._

/**
 * Java Client for using configuration service
 */
class JConfigService(configService: ConfigService, actorRuntime: ActorRuntime) extends IConfigService {

  import actorRuntime._

  override def create(path: Path,
                      configData: ConfigData,
                      annex: Boolean,
                      comment: String): CompletableFuture[ConfigId] =
    configService.create(path, configData, annex, comment).toJava.toCompletableFuture

  override def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    create(path, configData, annex = false, comment)

  override def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    configService.update(path, configData, comment).toJava.toCompletableFuture

  override def getById(path: Path, id: ConfigId): CompletableFuture[Optional[ConfigData]] =
    configService.getById(path, id).map(_.asJava).toJava.toCompletableFuture

  override def getLatest(path: Path): CompletableFuture[Optional[ConfigData]] =
    configService.getLatest(path).map(_.asJava).toJava.toCompletableFuture

  override def getByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]] =
    configService.getByTime(path, time).map(_.asJava).toJava.toCompletableFuture

  override def exists(path: Path): CompletableFuture[jl.Boolean] =
    configService.exists(path).toJava.toCompletableFuture.asInstanceOf[CompletableFuture[jl.Boolean]]

  override def exists(path: Path, id: ConfigId): CompletableFuture[jl.Boolean] =
    configService.exists(path, Some(id)).toJava.toCompletableFuture.asInstanceOf[CompletableFuture[jl.Boolean]]

  override def delete(path: Path, comment: String): CompletableFuture[Unit] =
    configService.delete(path, comment).toJava.toCompletableFuture

  override def list(fileType: FileType, pattern: String): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(Some(fileType), Some(pattern)).map(_.asJava).toJava.toCompletableFuture

  override def list(fileType: FileType): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(Some(fileType)).map(_.asJava).toJava.toCompletableFuture

  override def list(pattern: String): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(pattern = Some(pattern)).map(_.asJava).toJava.toCompletableFuture

  override def list(): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list().map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path,
                       from: Instant,
                       to: Instant,
                       maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    configService.history(path, from, to, maxResults = maxResults).map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path, from: Instant, to: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    configService.history(path, from, to, Int.MaxValue).map(_.asJava).toJava.toCompletableFuture

  override def history(path: Path, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, Instant.MIN, Instant.now, maxResults)

  override def history(path: Path): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, maxResults = Int.MaxValue)

  override def historyFrom(path: Path,
                           from: Instant,
                           maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, from, Instant.now, maxResults)

  override def historyFrom(path: Path, from: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, from, Instant.now, Int.MaxValue)

  override def historyUpTo(path: Path,
                           upTo: Instant,
                           maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, Instant.MIN, upTo, maxResults)

  override def historyUpTo(path: Path, upTo: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, Instant.MIN, upTo, Int.MaxValue)

  override def historyActive(path: Path,
                             from: Instant,
                             to: Instant,
                             maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    configService.historyActive(path, from, to, maxResults).map(_.asJava).toJava.toCompletableFuture

  override def historyActive(path: Path,
                             from: Instant,
                             to: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, from, to, Int.MaxValue)

  override def historyActive(path: Path, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, Instant.MIN, Instant.now, maxResults)

  override def historyActive(path: Path): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, Instant.MIN, Instant.now, Int.MaxValue)

  override def historyActiveFrom(path: Path,
                                 from: Instant,
                                 maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, from, Instant.now, maxResults)

  override def historyActiveFrom(path: Path, from: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, from, Instant.now, Int.MaxValue)

  override def historyActiveUpTo(path: Path,
                                 upTo: Instant,
                                 maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, Instant.MIN, upTo, maxResults)

  override def historyActiveUpTo(path: Path, upTo: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, Instant.MIN, upTo, Int.MaxValue)

  override def setActiveVersion(path: Path, id: ConfigId, comment: String): CompletableFuture[Unit] =
    configService.setActiveVersion(path, id, comment).toJava.toCompletableFuture

  override def resetActiveVersion(path: Path, comment: String): CompletableFuture[Unit] =
    configService.resetActiveVersion(path, comment).toJava.toCompletableFuture

  override def getActive(path: Path): CompletableFuture[Optional[ConfigData]] =
    configService.getActive(path).map(_.asJava).toJava.toCompletableFuture

  override def getActiveVersion(path: Path): CompletableFuture[Optional[ConfigId]] =
    configService.getActiveVersion(path).map(_.asJava).toJava.toCompletableFuture

  override def getActiveByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]] =
    configService.getActiveByTime(path, time).map(_.asJava).toJava.toCompletableFuture

  override def getMetadata: CompletableFuture[ConfigMetadata] = configService.getMetadata.toJava.toCompletableFuture

  override def asScala: ConfigService = configService

}
