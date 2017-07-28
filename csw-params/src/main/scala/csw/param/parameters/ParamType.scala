package csw.param.parameters

import csw.param.RaDec

sealed trait ParamType[T]

object ParamType {
  implicit object IntType   extends ParamType[Int]
  implicit object RaDecType extends ParamType[RaDec]
}
