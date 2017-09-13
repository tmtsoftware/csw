package csw.param

import com.google.protobuf.wrappers.BoolValue
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

  println(parameter1.items.get.values.map(x ⇒ BoolValue.parseFrom(x.toByteArray).value))

}
