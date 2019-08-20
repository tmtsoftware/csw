package csw.event.cli.utils

import csw.params.core.generics.Parameter
import csw.params.core.models.Struct

import scala.collection.mutable

case class PathSelector(paths: List[List[String]]) {
  def transform(paramSet: Set[Parameter[_]]): Set[Parameter[_]] = {
    StructCtx(Struct(paramSet), Nil).select.struct.paramSet
  }

  private case class StructCtx(struct: Struct, parents: List[String]) {
    def paramSet: Set[ParamCtx] = struct.paramSet.map(x => ParamCtx(x.asInstanceOf[Parameter[Any]], parents))

    def select: StructCtx = paths match {
      case Nil => this
      case _ =>
        val selectedSet = paramSet.filter(_.matchesPartialPath).map(_.select)
        copy(Struct(selectedSet.map(_.param)))
    }
  }

  private case class ParamCtx(param: Parameter[Any], parents: List[String]) {
    def matchesPartialPath: Boolean = paths.exists(_.startsWith(path))
    private def matchesFullPath: Boolean = paths.contains(path)

    def select: ParamCtx = {
      val selectedStructCtx = if (matchesFullPath) structsItems else structsItems.map(_.select)
      val selectedStructs = selectedStructCtx.filter(_.paramSet.nonEmpty).map(_.struct)
      copy(param = param.copy(items = selectedStructs ++ simpleItems))
    }

    private def simpleItems: mutable.ArraySeq[_] = param.items.filterNot(_.isInstanceOf[Struct])
    private def structsItems: mutable.ArraySeq[StructCtx] = param.items.collect {
      case x: Struct => StructCtx(x, path)
    }

    private def path: List[String] = parents :+ param.keyName
  }
}
