package csw.framework.scaladsl

import csw.framework.ComponentInfos
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ComponentHandlersFactoryTest extends AnyFunSuite with Matchers {
  test("Component Handlers factory should throw error when scala handler constructor not found | CSW-174") {
    val componentHandlerClassName = ComponentInfos.hcdInfoWithHandlerException.componentHandlerClassName
    val thrown = intercept[Exception] {
      ComponentHandlersFactory.make(componentHandlerClassName)
    }
    val expectedMessage = """
      |To load a component, you must provide one of the following:
      |For Scala: Subclass of class csw.framework.scaladsl.ComponentHandlers having constructor parameter types:
      |(interface akka.actor.typed.scaladsl.ActorContext, class csw.framework.models.CswContext)
      |OR
      |For Java: Subclass of class csw.framework.javadsl.JComponentHandlers having constructor parameter types:
      |(interface akka.actor.typed.javadsl.ActorContext, class csw.framework.models.JCswContext).
      |Received:
      |public csw.common.components.framework.InvalidComponentHandlers(csw.framework.models.CswContext,akka.actor.typed.scaladsl.ActorContext)
      |""".stripMargin
    assert(thrown.getClass === classOf[ClassCastException])
    assert(thrown.getMessage === expectedMessage)

  }

  test("Component Handlers factory should throw error when java handler constructor not found | CSW-174") {
    val componentHandlerClassName = ComponentInfos.jHcdInfoWithHandlerException.componentHandlerClassName
    val thrown = intercept[Exception] {
      ComponentHandlersFactory.make(componentHandlerClassName)
    }
    val expectedMessage =
      """
        |To load a component, you must provide one of the following:
        |For Scala: Subclass of class csw.framework.scaladsl.ComponentHandlers having constructor parameter types:
        |(interface akka.actor.typed.scaladsl.ActorContext, class csw.framework.models.CswContext)
        |OR
        |For Java: Subclass of class csw.framework.javadsl.JComponentHandlers having constructor parameter types:
        |(interface akka.actor.typed.javadsl.ActorContext, class csw.framework.models.JCswContext).
        |Received:
        |csw.framework.javadsl.components.JInvalidComponentHandlers(csw.framework.models.JCswContext,akka.actor.typed.javadsl.ActorContext)
        |""".stripMargin
    assert(thrown.getClass === classOf[ClassCastException])
    assert(thrown.getMessage === expectedMessage)
  }
}
