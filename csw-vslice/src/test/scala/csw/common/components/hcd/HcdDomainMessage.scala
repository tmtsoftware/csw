package csw.common.components.hcd

import csw.common.framework.models.DomainMsg

sealed trait HcdDomainMessage         extends DomainMsg
case class AxisStatistics(value: Int) extends HcdDomainMessage
