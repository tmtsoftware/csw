package csw.common.components.hcd

import csw.common.framework.models.DomainMsg

sealed trait HcdDomainMessages        extends DomainMsg
case class AxisStatistics(value: Int) extends HcdDomainMessages
