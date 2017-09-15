//package csw.param
//
//import com.google.protobuf.wrappers.Int32Value
//import csw.param.events.Setup2
//import csw.param.generics.{KeyType, Parameter}
//import csw.param.models.{ArrayData, MatrixData}
//import csw.param.pb.PbFormat
//import csw.units.Units
//import csw_params.keytype.{KeytypeProto, PbKeyType}
//import csw_params.parameter.{ParameterProto, PbParameter}
//import csw_params.parameter_types._
//import csw_params.units.PbUnits
//import org.scalatest.FunSuite
//
//class DD2 extends FunSuite {
//
//  test("1") {
//    val parameter: PbParameter = PbParameter()
//      .withName("encoder")
//      .withUnits(Units.centimeter)
//      .withKeyType(PbKeyType.IntKey)
//      .withItems(Items().withValues(Seq(1, 2).map(x => Int32Value().withValue(x).toByteString)))
//
//    val parameter1: PbParameter = PbParameter.parseFrom(parameter.toByteArray)
//
//    import spray.json.DefaultJsonProtocol._
//    val value: Parameter[Int] = PbFormat[Parameter[Int]].toCustom(parameter1.toByteString)
//
//    //    PbFormat.genericFormat[PbParameter, Parameter[Boolean]].toCustom(parameter1.toByteString)
//
//    //  println(parameter1.items.get.values.map(x ⇒ BoolValue.parseFrom(x.toByteArray).value))
//    println(value)
//    println(value.toJson)
//  }
//
//  test("2") {
//    val parameter: PbParameter = PbParameter()
//      .withName("encoder")
//      .withKeyType(PbKeyType.IntKey)
//      .withItems(Items().withValues(Seq(1, 2).map(x => Int32Value().withValue(x).toByteString)))
//
//    val parameter1: PbParameter = PbParameter.parseFrom(parameter.toByteArray)
//
//    import spray.json.DefaultJsonProtocol._
//    val value: Parameter[Int] = PbFormat[Parameter[Int]].toCustom(parameter1.toByteString)
//
//    //    PbFormat.genericFormat[PbParameter, Parameter[Boolean]].toCustom(parameter1.toByteString)
//
//    //  println(parameter1.items.get.values.map(x ⇒ BoolValue.parseFrom(x.toByteArray).value))
//    println(value)
//    println(value.toJson)
//  }
//
//  test("3") {
//    val pbFormat = PbFormat[Array[Array[Int]]]
//    val string   = pbFormat.toBase(Array(Array(1, 2, 3), Array(6, 7)))
//    val i        = pbFormat.toCustom(string)
//    println(string)
//    println(i.toList.map(_.toList))
//  }
//
//  test("4") {
//    val pbFormat = PbFormat.arrayTypeMapper[Array[Int]]
//    val string   = pbFormat.toBase(Array(Array(1, 2, 3), Array(6, 7)))
//    val i        = pbFormat.toCustom(string)
//    println(string)
//    println(i.toList.map(_.toList))
//  }
//
//  test("5") {
//    val pbFormat = PbFormat[MatrixData[Int]]
//    val string   = pbFormat.toBase(MatrixData.fromArrays(Array(Array(1, 2, 3), Array(6, 7))))
//    val i        = pbFormat.toCustom(string)
//    println(string)
//    println(i)
//  }
//
//  test("6") {
//    val key   = KeyType.IntKey.make("encoder")
//    val param = key.set(1, 2, 3, 4)
//
//    import spray.json.DefaultJsonProtocol._
//    val pbFormat = PbFormat[Parameter[Int]]
//    val value    = pbFormat.toBase(param)
//    val out      = pbFormat.toCustom(value)
//
//    println(value)
//    println(out)
//    println(out == param)
//
//    val key2   = KeyType.StringKey.make("encoder")
//    val param2 = key2.set("abc", "xyc")
//
//    import spray.json.DefaultJsonProtocol._
//    val pbFormat2 = PbFormat[Parameter[String]]
//    val value2    = pbFormat2.toBase(param2)
//    val out2      = pbFormat2.toCustom(value2)
//
//    println(value2)
//    println(out2)
//    println(out2 == param2)
//
//    val pbFormat3 = PbFormat[Setup2[Int]]
//    val param3    = Setup2(Array(param, param))
//    val value3    = pbFormat3.toBase(param3)
//    val out3      = pbFormat3.toCustom(value3)
//
//    println(value3)
//    println(out3)
//    println(out3 == param3)
//  }
//
//  test("7") {
//    BooleanItems().addValues(true, false)
////    val str = ParameterTypesProto.keyType.get(BoolItems.scalaDescriptor.getOptions)
//    println(BooleanItems().keyType)
////    println(str)
//  }
//
//  test("8") {
//    println(IntItems().addValues(1, 2, 3).as[ArrayData[Int]])
//    println(IntArrayItems().addValues(ArrayData.fromArray(1, 2, 3), ArrayData.fromArray(6, 7)).as[MatrixData[Int]])
//  }
//}
