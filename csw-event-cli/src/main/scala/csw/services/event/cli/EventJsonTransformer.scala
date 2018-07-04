package csw.services.event.cli

import akka.japi.Option.Some
import ujson.Js

class EventJsonTransformer(printLine: Any ⇒ Unit, options: Options) {

  def transformInPlace(eventJson: Js.Obj, paths: List[String]): Unit =
    transformInPlace0(eventJson, None, paths)

  private def transformInPlace0(json: Js.Obj, parentPath: Option[String], paths: List[String]): Unit =
    paths match {
      case Nil ⇒
      case _ ⇒
        def fullPath(json: Js.Value): String = {
          val keyName = json("keyName").str
          if (parentPath.isDefined) s"${parentPath.get}/$keyName"
          else keyName
        }

        json("paramSet") = json("paramSet").arr.filter(param ⇒ {
          val currentFullPath = fullPath(param)
          paths.contains(currentFullPath) || paths.exists(path => path.startsWith(s"$currentFullPath/"))
        })

        json("paramSet").arr.foreach { param =>
          val currentFullPath = fullPath(param)
          if (!paths.contains(currentFullPath))
            param("values") = param("values").arr.filter {
              case value: Js.Obj =>
                transformInPlace0(value, Some(currentFullPath), paths)
                value("paramSet").arr.nonEmpty //Remove empty paramSets
              case _ => true
            } else if (options.isOneline) {
            var values = s"$currentFullPath = ${param("values").arr.mkString("[", ",", "]")}"
            if (options.printUnits) values += s" [${param("units").str}]"
            printLine(values)
          }
        }
    }
}
