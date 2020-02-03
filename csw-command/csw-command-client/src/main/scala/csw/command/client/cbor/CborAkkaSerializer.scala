package csw.command.client.cbor

import akka.serialization.Serializer
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}
import org.reflections.Reflections

import scala.jdk.CollectionConverters.SetHasAsScala
import scala.reflect.ClassTag

abstract class CborAkkaSerializer[Ser: ClassTag] extends Serializer {
  private val logger: Logger = GenericLoggerFactory.getLogger

  private var registrations: Map[Class[_], Codec[_]] = Map.empty

  private val reflections: Reflections = {
    val packageName = scala.reflect.classTag[Ser].runtimeClass.getPackageName
    val basePackage = packageName.split("\\.").head
    new Reflections(basePackage)
  }

  protected def register[T <: Ser: Encoder: Decoder: ClassTag]: Unit = {
    val clazz    = scala.reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]]
    val subtypes = reflections.getSubTypesOf[T](clazz).asScala.toSet
    (subtypes + clazz).foreach(registrations += _ -> Codec.of[T])
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
    registrations.getOrElse(classValue, {
      val ex = new RuntimeException(s"does not support $action of $classValue")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    })
  }
}
