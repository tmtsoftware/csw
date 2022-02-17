package csw.framework.scaladsl

import java.lang.reflect.Constructor

private[framework] object ClassHelpers {

  def verifyClass(inputHandlerClass: Class[?], requiredConstructor: Constructor[?]): Boolean = {
    requiredConstructor.getDeclaringClass.isAssignableFrom(inputHandlerClass) &&
    inputHandlerClass.getDeclaredConstructors.exists(constructor =>
      requiredConstructor.getParameterTypes.sameElements(constructor.getParameterTypes)
    )
  }

  def getConstructorFor(clazz: Class[?], consArgs: Seq[Class[?]]): Constructor[?] = {
    val constructor = clazz.getDeclaredConstructor(consArgs*)
    constructor.setAccessible(true)
    constructor
  }

}
