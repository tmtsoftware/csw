//package csw.param.parameters
//
//import csw.param.RaDec
//
//sealed trait ParamType[T]
//
//object ParamType {
//  def apply[T](implicit x: ParamType[T]): ParamType[T] = x
//
//  implicit case object IntParameter     extends ParamType[Int]
//  implicit case object BooleanParameter extends ParamType[Boolean]
//  implicit case object RaDecParameter   extends ParamType[RaDec]
//}
