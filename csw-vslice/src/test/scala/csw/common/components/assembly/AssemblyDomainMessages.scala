package csw.common.components.assembly

import csw.common.framework.models.RunningMsg.DomainMsg

sealed trait AssemblyDomainMsg extends DomainMsg
case object OperationsMode     extends AssemblyDomainMsg
