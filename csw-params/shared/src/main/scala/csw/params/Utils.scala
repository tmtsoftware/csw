package csw.params

import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.ObsId

object Utils {
  def getClassName[T](v: T): String = {
    val simpleName = v.getClass.getSimpleName
    if (simpleName.last == '$') simpleName.dropRight(1) else simpleName
  }

  def obsIdParam(obsId: ObsId): Parameter[String]            = StringKey.make("obsId").set(obsId.obsId)
  def exposureIdParam(exposureId: String): Parameter[String] = StringKey.make("exposureId").set(exposureId)

}
