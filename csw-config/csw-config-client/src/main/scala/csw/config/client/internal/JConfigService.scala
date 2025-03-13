/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.client.internal

import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{util, lang => jl}

import csw.config.api.ConfigData
import csw.config.api.javadsl.IConfigService
import csw.config.api.scaladsl.ConfigService
import csw.config.models.{ConfigFileInfo, ConfigFileRevision, ConfigId, ConfigMetadata, FileType}

import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.jdk.OptionConverters.*

/**
 * Java Client for using configuration service
 */
private[config] class JConfigService(configService: ConfigService, actorRuntime: ActorRuntime) extends IConfigService {

  import actorRuntime._

  override def create(path: Path, configData: ConfigData, annex: Boolean, comment: String): CompletableFuture[ConfigId] =
    configService.create(path, configData, annex, comment).asJava.toCompletableFuture

  override def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    create(path, configData, annex = false, comment)

  override def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId] =
    configService.update(path, configData, comment).asJava.toCompletableFuture

  override def getById(path: Path, id: ConfigId): CompletableFuture[Optional[ConfigData]] =
    configService.getById(path, id).map(_.toJava).asJava.toCompletableFuture

  override def getLatest(path: Path): CompletableFuture[Optional[ConfigData]] =
    configService.getLatest(path).map(_.toJava).asJava.toCompletableFuture

  override def getByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]] =
    configService.getByTime(path, time).map(_.toJava).asJava.toCompletableFuture

  override def exists(path: Path): CompletableFuture[jl.Boolean] =
    configService.exists(path).asJava.toCompletableFuture.asInstanceOf[CompletableFuture[jl.Boolean]]

  override def exists(path: Path, id: ConfigId): CompletableFuture[jl.Boolean] =
    configService.exists(path, Some(id)).asJava.toCompletableFuture.asInstanceOf[CompletableFuture[jl.Boolean]]

  override def delete(path: Path, comment: String): CompletableFuture[Unit] =
    configService.delete(path, comment).asJava.toCompletableFuture

  override def list(fileType: FileType, pattern: String): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(Some(fileType), Some(pattern)).map(_.asJava).asJava.toCompletableFuture

  override def list(fileType: FileType): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(Some(fileType)).map(_.asJava).asJava.toCompletableFuture

  override def list(pattern: String): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list(pattern = Some(pattern)).map(_.asJava).asJava.toCompletableFuture

  override def list(): CompletableFuture[util.List[ConfigFileInfo]] =
    configService.list().map(_.asJava).asJava.toCompletableFuture

  override def history(
      path: Path,
      from: Instant,
      to: Instant,
      maxResults: Int
  ): CompletableFuture[util.List[ConfigFileRevision]] =
    configService.history(path, from, to, maxResults = maxResults).map(_.asJava).asJava.toCompletableFuture

  override def history(path: Path, from: Instant, to: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    configService.history(path, from, to, Int.MaxValue).map(_.asJava).asJava.toCompletableFuture

  override def history(path: Path, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, Instant.MIN, Instant.now, maxResults)

  override def history(path: Path): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, maxResults = Int.MaxValue)

  override def historyFrom(path: Path, from: Instant, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, from, Instant.now, maxResults)

  override def historyFrom(path: Path, from: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, from, Instant.now, Int.MaxValue)

  override def historyUpTo(path: Path, upTo: Instant, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, Instant.MIN, upTo, maxResults)

  override def historyUpTo(path: Path, upTo: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    history(path, Instant.MIN, upTo, Int.MaxValue)

  override def historyActive(
      path: Path,
      from: Instant,
      to: Instant,
      maxResults: Int
  ): CompletableFuture[util.List[ConfigFileRevision]] =
    configService.historyActive(path, from, to, maxResults).map(_.asJava).asJava.toCompletableFuture

  override def historyActive(path: Path, from: Instant, to: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, from, to, Int.MaxValue)

  override def historyActive(path: Path, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, Instant.MIN, Instant.now, maxResults)

  override def historyActive(path: Path): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, Instant.MIN, Instant.now, Int.MaxValue)

  override def historyActiveFrom(path: Path, from: Instant, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, from, Instant.now, maxResults)

  override def historyActiveFrom(path: Path, from: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, from, Instant.now, Int.MaxValue)

  override def historyActiveUpTo(path: Path, upTo: Instant, maxResults: Int): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, Instant.MIN, upTo, maxResults)

  override def historyActiveUpTo(path: Path, upTo: Instant): CompletableFuture[util.List[ConfigFileRevision]] =
    historyActive(path, Instant.MIN, upTo, Int.MaxValue)

  override def setActiveVersion(path: Path, id: ConfigId, comment: String): CompletableFuture[Unit] =
    configService.setActiveVersion(path, id, comment).asJava.toCompletableFuture

  override def resetActiveVersion(path: Path, comment: String): CompletableFuture[Unit] =
    configService.resetActiveVersion(path, comment).asJava.toCompletableFuture

  override def getActive(path: Path): CompletableFuture[Optional[ConfigData]] =
    configService.getActive(path).map(_.toJava).asJava.toCompletableFuture

  override def getActiveVersion(path: Path): CompletableFuture[Optional[ConfigId]] =
    configService.getActiveVersion(path).map(_.toJava).asJava.toCompletableFuture

  override def getActiveByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]] =
    configService.getActiveByTime(path, time).map(_.toJava).asJava.toCompletableFuture

  override def getMetadata: CompletableFuture[ConfigMetadata] = configService.getMetadata.asJava.toCompletableFuture

  override def asScala: ConfigService = configService

}
