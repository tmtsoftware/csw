package csw.param.parameters

sealed trait ParamType[T]

object ParamType {
  implicit object IntParam extends ParamType[Int]
}
