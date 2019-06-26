package csw.location.client

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypeRange, MediaType}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import io.bullet.borer.{Decoder, Encoder, Json}
import io.bullet.borer.compat.akka._

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
      .compose(Json.encode(_).to[ByteString].bytes)
  }
}
