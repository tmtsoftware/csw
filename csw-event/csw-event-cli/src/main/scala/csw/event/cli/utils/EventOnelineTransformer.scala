/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.cli.utils

import csw.event.cli.args.Options
import csw.params.core.generics.Parameter
import csw.params.events.Event

class EventOnelineTransformer(options: Options) {

  def transform(events: Seq[Event]): List[String] = {
    val onelines = events.flatMap(e => transform(e)).toList
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

  private def traverse(params: Set[Parameter[?]], paths: List[String]): List[Oneline] =
    params.toList.flatMap { param =>
      val currentPath = param.keyName

      param.keyType match {
        case _ if paths.isEmpty || paths.contains(currentPath) =>
          List(Oneline(currentPath, param))
        case _ => Nil
      }
    }

}
