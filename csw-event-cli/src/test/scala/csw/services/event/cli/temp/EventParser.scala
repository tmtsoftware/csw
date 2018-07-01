package csw.services.event.cli.temp

import akka.japi.Option.Some
import play.api.libs.json.JsValue
import ujson.Js

object EventParser {
  def parse(input: JsValue, path: String): JsValue = {
    input
  }

  def parse(input: Js.Obj, path: String): Js.Obj = {
    transformInPlace(input, path.split("/").toList)
    input
  }

  private def transformInPlace(json: Js.Obj, names: List[String]): Unit = names match {
    case Nil =>
    case name :: rest =>
      json("paramSet") = json("paramSet").arr.filter(_("keyName").str == name)
      json("paramSet").arr.foreach { innerSet =>
        innerSet("values").arr.foreach {
          case x: Js.Obj => transformInPlace(x, rest)
          case _         =>
        }
      }
  }

  def inspect(json: Js.Obj, printLine: Any ⇒ Unit): Unit = {
    def inspect0(js: Js, parentKey: Option[String]): Unit = {
      js("paramSet").arr.foreach { innerSet ⇒
        val keyName = innerSet("keyName").str
        val keyType = innerSet("keyType").str
        val unit    = innerSet("units").str

        val fullKey =
          if (parentKey.isDefined) s"${parentKey.get}/$keyName"
          else keyName

        printLine(s"$fullKey = $keyType[$unit]")

        innerSet("values").arr.foreach {
          case x: Js.Obj => inspect0(x, Some(fullKey))
          case _         ⇒
        }
      }
    }

    inspect0(json, None)
  }

  def parseWithMultipleKeys(input: Js.Obj, path: List[String]): Js.Obj = {
    val paths = path.map(_.split("/").toList)
    transformInPlace0(input, None, buildIncrementalPath(paths))
    input
  }

  private def buildIncrementalPath(paths: List[List[String]]) = paths.map { p1 ⇒
    var last = ""
    p1.map { p2 ⇒
      last = if (last.nonEmpty) last + "/" + p2 else p2
      last
    }
  }

  private def transformInPlace0(json: Js.Obj, parentKey: Option[String], names: List[List[String]]): Unit = names match {
    case Nil ⇒
    case _ ⇒
      val (keys, rest) = names.map {
        case Nil          ⇒ ("", Nil)
        case head :: tail ⇒ (head, tail)
      }.unzip

      keys.filter(_.nonEmpty) match {
        case Nil =>
        case _ =>
          def keyName(json: Js.Value): String = {
            val keyName = json("keyName").str
            if (parentKey.isDefined) s"${parentKey.get}/$keyName"
            else keyName
          }

          json("paramSet") = json("paramSet").arr.filter(x ⇒ keys.contains(keyName(x)))

          json("paramSet").arr.foreach { innerSet =>
            innerSet("values").arr.foreach {
              case x: Js.Obj => transformInPlace0(x, Some(keyName(innerSet)), rest)
              case _         =>
            }
          }
      }
  }

}
