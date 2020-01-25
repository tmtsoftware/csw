package csw.command.client.cbor

import akka.serialization.Serializer
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}

import scala.reflect.ClassTag

trait CborAkkaSerializer[Ser] extends Serializer {
  private val logger: Logger = GenericLoggerFactory.getLogger

  private var registrations: List[(Class[_], Codec[_])] = Nil

  protected def register[T <: Ser: Encoder: Decoder: ClassTag]: Unit = {
    registrations ::= scala.reflect.classTag[T].runtimeClass -> Codec.of[T]
  }

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = getCodec(o.getClass) match {
    case Some(codec) =>
      val encoder = codec.encoder.asInstanceOf[Encoder[AnyRef]]
      Cbor.encode(o)(encoder).toByteArray
    case None =>
      val ex = new RuntimeException(s"does not support encoding of ${o.getClass}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = getCodec(manifest.get) match {
    case Some(codec) =>
      val decoder = codec.decoder.asInstanceOf[Decoder[AnyRef]]
      Cbor.decode(bytes).to[AnyRef](decoder).value
    case None =>
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  private def getCodec(classValue: Class[_]): Option[Codec[_]] = registrations.collectFirst {
    case (clazz, codec) if clazz.isAssignableFrom(classValue) => codec
  }
}
