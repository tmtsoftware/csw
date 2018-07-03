package csw.services.event.cli

import akka.japi.Option.Some
import ujson.Js

object EventJsonTransformer {

  def transformInPlace(eventJson: Js.Obj, paths: List[List[String]]): Unit =
    transformInPlace0(eventJson, None, buildIncrementalPath(paths))

  private def buildIncrementalPath(paths: List[List[String]]) = paths.map { path ⇒
    var last = ""
    path.map { segment ⇒
      last = if (last.nonEmpty) last + "/" + segment else segment
      last
    }
  }

  private def breakIncrementalPaths(allIncrementalPaths: List[List[String]]) = {
    allIncrementalPaths.map {
      case Nil                                            ⇒ ("", Nil)
      case currentIncrementalPath :: nextIncrementalPaths ⇒ (currentIncrementalPath, nextIncrementalPaths)
    }.unzip
  }

  private def transformInPlace0(json: Js.Obj, parentPath: Option[String], incrementalPaths: List[List[String]]): Unit =
    incrementalPaths match {
      case Nil ⇒
      case _ ⇒
        val (currentIncrementalPaths, allNextIncrementalPaths) = breakIncrementalPaths(incrementalPaths)

        currentIncrementalPaths.filter(_.nonEmpty) match {
          case Nil =>
          case _ =>
            def currentPath(json: Js.Value): String = {
              val keyName = json("keyName").str
              if (parentPath.isDefined) s"${parentPath.get}/$keyName"
              else keyName
            }

            json("paramSet") = json("paramSet").arr.filter(param ⇒ currentIncrementalPaths.contains(currentPath(param)))

            json("paramSet").arr.foreach { param =>
              param("values") = param("values").arr.filter {
                case value: Js.Obj =>
                  transformInPlace0(value, Some(currentPath(param)), allNextIncrementalPaths)
                  value("paramSet").arr.nonEmpty //Remove empty paramSets
                case _ => true
              }
            }
        }
    }
}
