package csw.params

object Utils {
  def getClassName[T](v: T): String = {
    val simpleName = v.getClass.getSimpleName
    if (simpleName.last == '$') simpleName.dropRight(1) else simpleName
  }
}
