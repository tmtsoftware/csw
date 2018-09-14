package csw.services.event.cli.utils

import csw.params.events.Event
import csw.params.core.generics.KeyType.StructKey
import csw.params.core.generics.Parameter
import csw.params.core.models.Struct
import csw.services.event.cli.args.Options

class EventOnelineTransformer(options: Options) {

  def transform(events: Seq[Event]): List[String] = {
    val onelines = events.flatMap(e ⇒ transform(e)).toList
    if (options.isTerseOut) onelines else Formatter.EventSeparator :: onelines
  }

  def transform(event: Event): List[String] = {
    val onelineFormatter = OnelineFormatter(options)
    val onelines =
      if (event.isInvalid) Formatter.invalidKey(event.eventKey)
      else onelineFormatter.format(traverse(event.paramSet, options.paths(event.eventKey)))

    if (options.isOnelineOut) List(onelineFormatter.header(event), onelines, Formatter.EventSeparator)
    else List(onelines)
  }

  private def makeCurrentPath(param: Parameter[_], parentKey: Option[String]) =
    if (parentKey.isDefined) s"${parentKey.get}/${param.keyName}"
    else param.keyName

  private def traverse(params: Set[Parameter[_]], paths: List[String], parentKey: Option[String] = None): List[Oneline] =
    params.toList.flatMap { param ⇒
      val currentPath = makeCurrentPath(param, parentKey)

      param.keyType match {
        case StructKey ⇒
          val nestedParams = param.values.flatMap(_.asInstanceOf[Struct].paramSet).toSet
          traverse(nestedParams, paths, Some(currentPath))
        case _ if paths.isEmpty || paths.contains(currentPath) || paths.exists(p ⇒ currentPath.contains(p)) ⇒
          List(Oneline(currentPath, param))
        case _ ⇒ Nil
      }
    }

}
