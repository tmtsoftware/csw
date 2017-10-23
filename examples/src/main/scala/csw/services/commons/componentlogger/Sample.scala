package csw.services.commons.componentlogger

import csw.services.logging.scaladsl.ComponentLogger

//#component-logger
class Sample(_componentName: String) extends ComponentLogger.Simple {

  override protected def componentName(): String = _componentName

}
//#component-logger
