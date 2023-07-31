/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server.http

import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.MediaTypes.`application/json`
import org.apache.pekko.http.scaladsl.model.{ContentTypeRange, ContentTypes, HttpEntity, MediaType}
import org.apache.pekko.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import org.apache.pekko.util.ByteString
import csw.config.api.ConfigData
import io.bullet.borer.compat.pekko.*
import io.bullet.borer.{Decoder, Encoder, Json}

import scala.collection.immutable.Seq

object HttpCodecs extends HttpCodecs
trait HttpCodecs {

  lazy val mediaTypes: Seq[MediaType.WithFixedCharset]     = List(`application/json`)
  lazy val unmarshallerContentTypes: Seq[ContentTypeRange] = mediaTypes.map(ContentTypeRange.apply)

  implicit def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .map(Json.decode(_).to[A].value)
  }

  implicit def marshaller[A: Encoder]: ToEntityMarshaller[A] = {
    Marshaller
      .oneOf(mediaTypes: _*)(Marshaller.byteStringMarshaller(_))
      .compose(Json.encode(_).to[ByteString].result)
  }

  // This marshaller is used to create a response stream for get/getActive requests
  implicit val configDataMarshaller: ToEntityMarshaller[ConfigData] = Marshaller.opaque { configData =>
    HttpEntity(ContentTypes.`application/octet-stream`, configData.length, configData.source)
  }

  implicit val configDataUnmarshaller: FromEntityUnmarshaller[String] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(ContentTypes.`application/octet-stream`)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset)       => data.decodeString(charset.nioCharset.name)
      }
}
