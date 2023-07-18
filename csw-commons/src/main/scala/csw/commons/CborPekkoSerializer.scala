/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.commons

import org.apache.pekko.serialization.Serializer
import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}

import scala.reflect.ClassTag

abstract class CborPekkoSerializer[Ser] extends Serializer {

  private var registrations: List[(Class[_], Codec[_])] = Nil

  protected def register[T <: Ser: Encoder: Decoder: ClassTag]: Unit = {
    registrations ::= scala.reflect.classTag[T].runtimeClass -> Codec.of[T]
  }

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = {
    val codec   = getCodec(o.getClass, "encoding")
    val encoder = codec.encoder.asInstanceOf[Encoder[AnyRef]]
    Cbor.encode(o)(encoder).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val codec   = getCodec(manifest.get, "decoding")
    val decoder = codec.decoder.asInstanceOf[Decoder[AnyRef]]
    Cbor.decode(bytes).to[AnyRef](decoder).value
  }

  private def getCodec(classValue: Class[_], action: String): Codec[_] = {
    registrations
      .collectFirst {
        case (clazz, codec) if clazz.isAssignableFrom(classValue) => codec
      }
      .getOrElse {
        throw new RuntimeException(s"$action of $classValue is not configured")
      }
  }
}
