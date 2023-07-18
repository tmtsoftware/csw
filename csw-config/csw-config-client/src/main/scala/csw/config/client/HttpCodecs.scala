/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.client

import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.MediaTypes.`application/json`
import org.apache.pekko.http.scaladsl.model.{ContentTypeRange, MediaType}
import org.apache.pekko.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import org.apache.pekko.util.ByteString
import io.bullet.borer.compat.pekko._
import io.bullet.borer.{Decoder, Encoder, Json}

import scala.collection.immutable.Seq

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
}
