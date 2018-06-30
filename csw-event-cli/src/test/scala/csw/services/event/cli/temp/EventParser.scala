package csw.services.event.cli.temp

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

  def parseWithMultipleKeys(input: Js.Obj, path: List[String]): Js.Obj = {
    transformInPlace0(input, path.map(_.split("/").toList))
    input
  }

  private def transformInPlace0(json: Js.Obj, names: List[List[String]]): Unit = names match {
    case Nil ⇒
    case _ ⇒
      val (keys, rest) = names.map {
        case Nil          ⇒ ("", Nil)
        case head :: tail ⇒ (head, tail)
      }.unzip

      keys match {
        case Nil =>
        case _ =>
          json("paramSet") = json("paramSet").arr.filter(x ⇒ keys.contains(x("keyName").str))
          json("paramSet").arr.foreach { innerSet =>
            innerSet("values").arr.foreach {
              case x: Js.Obj => transformInPlace0(x, rest)
              case _         =>
            }
          }
      }
  }

}
