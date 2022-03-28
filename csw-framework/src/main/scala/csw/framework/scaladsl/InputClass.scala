/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.scaladsl

import java.lang.reflect.Constructor
import scala.reflect.{ClassTag, classTag}

private[framework] class InputClass(val inputClass: Class[?]) {

  def isValid[H: ClassTag]: Boolean = {
    val handlerClass = classTag[H].runtimeClass
    handlerClass.isAssignableFrom(inputClass) &&
    paramsOf(inputClass).sameElements(paramsOf(handlerClass))
  }

  def instantiateAs[H](ctx: Any, cswCtx: Any): H = {
    constructorOf(inputClass).newInstance(ctx, cswCtx).asInstanceOf[H]
  }

  private def paramsOf(clazz: Class[?]): Array[Class[?]] = {
    constructorOf(clazz).getParameterTypes
  }

  private def constructorOf(clazz: Class[?]): Constructor[?] = {
    val constructor = clazz.getDeclaredConstructors.head
    constructor.setAccessible(true)
    constructor
  }

}
