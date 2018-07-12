package csw.services.event.cli.utils

import csw.messages.events.Event
import csw.messages.params.generics.KeyType.StructKey
import csw.messages.params.generics.Parameter
import csw.messages.params.models.Struct
import csw.services.event.cli.args.Options

class EventOnelineTransformer(options: Options) {

  def transform(events: Seq[Event]): List[String] =
    Formatter.eventSeparator :: events.flatMap(e ⇒ transform(e)).toList

  def transform(event: Event): List[String] = {
    val onelineFormatter = OnelineFormatter(options)
    val eventHeader      = onelineFormatter.header(event)

    val onelines =
      if (event.isInvalid) Formatter.invalidKey(event.eventKey)
      else onelineFormatter.format(event, traverse(event.paramSet, options.paths(event.eventKey)))

    List(eventHeader, onelines, Formatter.eventSeparator)
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
