package csw.services.commons.componentlogger

import csw.services.logging.scaladsl.FrameworkLogger

//#component-logger
class Sample(_componentName: String) extends FrameworkLogger.Simple {

  override protected def componentName(): String = _componentName

}
//#component-logger
