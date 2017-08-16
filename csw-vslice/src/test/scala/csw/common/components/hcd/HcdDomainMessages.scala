package csw.common.components.hcd

import csw.common.framework.models.RunningMsg.DomainMsg

sealed trait HcdDomainMsg                      extends DomainMsg
case class SampleHcdAxisStatistics(value: Int) extends HcdDomainMsg
