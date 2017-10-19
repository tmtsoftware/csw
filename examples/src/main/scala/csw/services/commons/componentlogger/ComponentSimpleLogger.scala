package csw.services.commons.componentlogger

import csw.services.logging.scaladsl.ComponentLogger

//#component-logger
class ComponentSimpleLogger(_componentName: String) extends ComponentLogger.Simple {

  override protected def componentName(): String = _componentName

}
//#component-logger
