package csw.param

import com.google.protobuf.ByteString
import com.google.protobuf.wrappers.BoolValue
import com.trueaccord.scalapb.TypeMapper
import csw.param.generics.Parameter
import csw.param.pb.PbFormat
import csw_params.keytype.PbKeyType
import csw_params.parameter.PbParameter
import csw_params.parameter_types.{Booleans, Items}
import csw_params.units.PbUnits

object DD2 extends App {

  private val parameter: PbParameter = PbParameter()
    .withName("encoder")
    .withUnits(PbUnits.centimeter)
    .withKeyType(PbKeyType.BooleanKey)
    .withItems(Items().withValues(Seq(true, false).map(x => BoolValue().withValue(x).toByteString)))

  private val parameter1: PbParameter = PbParameter.parseFrom(parameter.toByteArray)

  import spray.json.DefaultJsonProtocol._
  private val value: Parameter[Boolean] = PbFormat[Parameter[Boolean]].toCustom(parameter1.toByteString)

  //    PbFormat.genericFormat[PbParameter, Parameter[Boolean]].toCustom(parameter1.toByteString)

//  println(parameter1.items.get.values.map(x ⇒ BoolValue.parseFrom(x.toByteArray).value))
  println(value)

}
