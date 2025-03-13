/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server.http

import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.regex.{Pattern, PatternSyntaxException}

import org.apache.pekko.http.scaladsl.model.headers.HttpEncoding
import org.apache.pekko.http.scaladsl.server.*
import org.apache.pekko.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import csw.config.api.ConfigData
import csw.config.api.commons.TokenMaskSupport
import csw.config.models.{ConfigId, FileType}
import csw.config.server.commons.{ConfigServerLogger, PathValidator}
import csw.logging.api.scaladsl.Logger

/**
 * Helper class for ConfigServiceRoute
 */
trait HttpParameter extends TokenMaskSupport with Directives with HttpCodecs {

  def prefix(prefix: String): Directive1[Path] =
    path(prefix / Remaining).flatMap { path =>
      validate(PathValidator.isValid(path), PathValidator.message(path)).tmap[Path] { _ => Paths.get(path) }
    }

  override val logger: Logger = ConfigServerLogger.getLogger

  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => maskRequest andThen logRequest))

  val idParam: Directive1[Option[ConfigId]]  = parameter("id".?).map(_.map(new ConfigId(_)))
  val dateParam: Directive1[Option[Instant]] = parameter("date".?).map(_.map(Instant.parse))
  val fromParam: Directive1[Instant]         = parameter("from".?).map(_.map(Instant.parse).getOrElse(Instant.MIN))
  val toParam: Directive1[Instant]           = parameter("to".?).map(_.map(Instant.parse).getOrElse(Instant.now()))
  val maxResultsParam: Directive1[Int]       = parameter("maxResults".as[Int] ? Int.MaxValue)
  val commentParam: Directive1[String]       = parameter("comment" ? "")
  val annexParam: Directive1[Boolean]        = parameter("annex".as[Boolean] ? false)

  // pattern is an optional parameter coming with list request
  // for list request if the pattern is provided, then it is first compiled and then forwarded to business code to process the request
  // if the pattern provided throws `PatternSyntaxException` then immediate response of `BadRequest` is sent back to client
  val patternParam: Directive1[Option[String]] = parameter("pattern".?).flatMap {
    case p @ Some(pattern) =>
      try {
        Pattern.compile(pattern)
        provide(p)
      }
      catch {
        case ex: PatternSyntaxException => reject(MalformedQueryParamRejection("pattern", ex.getMessage))
      }
    case None => provide(None)
  }

  // type is an optional parameter coming with list request
  // for list request if the type is provided, then it is first casted to one of the available types ('Annex' and 'Normal')
  // and then forwarded to business code to process the request
  // if the type provided throws `PatternSyntaxException` then immediate response of `BadRequest` is sent back to client
  val typeParam: Directive1[Option[FileType]] = parameter("type".?).flatMap {
    case Some(fileType) =>
      FileType.withNameInsensitiveOption(fileType) match {
        case ft @ Some(_) => provide(ft)
        case None         => reject(MalformedQueryParamRejection("type", s"Supported types: ${FileType.stringify}"))
      }
    case None => provide(None)
  }

  val configDataEntity: Directive1[ConfigData] = extractRequestEntity.flatMap {
    case entity if entity.contentLengthOption.isDefined =>
      provide(ConfigData.from(entity.dataBytes, entity.contentLengthOption.get))
    case _ =>
      reject(UnsupportedRequestEncodingRejection(HttpEncoding.custom("All encodings with contentLength value")))
  }
}
