package csw.param.events

import com.trueaccord.scalapb.TypeMapper
import csw.param.generics.Parameter
import csw.param.pb.PbFormat
import csw_params.parameter.PbSetup
import spray.json.JsonFormat

import scala.reflect.ClassTag

case class Setup2[S: JsonFormat: ClassTag](params: Array[Parameter[S]])

object Setup2 {
  implicit def typeMapper[S: ClassTag: JsonFormat: PbFormat]: TypeMapper[PbSetup, Setup2[S]] =
    new TypeMapper[PbSetup, Setup2[S]] {
      override def toCustom(base: PbSetup): Setup2[S] = {
        Setup2(PbFormat[Array[Parameter[S]]].toCustom(base.toByteString))
      }

      override def toBase(custom: Setup2[S]): PbSetup = {
        PbSetup.parseFrom(PbFormat[Array[Parameter[S]]].toBase(custom.params).toByteArray)
      }
    }
}
