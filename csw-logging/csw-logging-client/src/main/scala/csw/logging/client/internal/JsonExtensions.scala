/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.internal

import play.api.libs.json.*

import scala.annotation.nowarn

object JsonExtensions {
  implicit class AnyToJson(val x: Any) extends AnyVal {
    @nowarn
    def asJson: JsValue =
      x match {
        case x1: Int              => JsNumber(x1)
        case x1: Long             => JsNumber(x1)
        case x1: Float            => JsNumber(x1.toDouble)
        case x1: Double           => JsNumber(x1)
        case x: Boolean           => JsBoolean(x)
        case x: String            => JsString(x)
        case null                 => JsNull
        case xs: Seq[Any]         => JsArray(xs.map(_.asJson))
        case xs: Map[String, Any] => JsObject(xs.view.mapValues(_.asJson).toMap)
        case _                    => JsString(x.toString)
      }
  }

  implicit class AnyMapToJson(val xs: Map[String, Any]) extends AnyVal {
    def asJsObject: JsObject = JsObject(xs.view.mapValues(_.asJson).toMap)
  }

  implicit class RichJsObject(val xs: JsObject) extends AnyVal {
    def getString(key: String): String =
      xs.value.getOrElse(key, JsString("")) match {
        case x: JsString => x.value
        case x           => x.toString()
      }

    def contains(key: String): Boolean = xs.keys.contains(key)

  }

}
