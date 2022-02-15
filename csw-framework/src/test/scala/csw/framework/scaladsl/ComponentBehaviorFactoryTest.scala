package csw.framework.scaladsl

import csw.framework.ComponentInfos
import csw.framework.models.ComponentHandlerNotFoundException
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ComponentBehaviorFactoryTest extends AnyFunSuite with Matchers {
  test("Component Behavior factory should throw error when scala handler constructor not found | CSW-174") {
    val componentHandlerClassName = ComponentInfos.hcdInfoWithHandlerException.componentHandlerClassName
    val thrown = intercept[Exception] {
      ComponentBehaviorFactory.make(componentHandlerClassName)
    }
    assert(thrown.getClass === classOf[ComponentHandlerNotFoundException])
  }

  test("Component Behavior factory should throw error when java handler constructor not found | CSW-174") {
    val componentHandlerClassName = ComponentInfos.jHcdInfoWithHandlerException.componentHandlerClassName
    val thrown = intercept[Exception] {
      ComponentBehaviorFactory.make(componentHandlerClassName)
    }
    assert(thrown.getClass === classOf[ComponentHandlerNotFoundException])
  }
}
