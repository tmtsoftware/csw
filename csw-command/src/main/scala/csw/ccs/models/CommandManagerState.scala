package csw.ccs.models

import csw.messages.params.models.RunId

case class CommandManagerState(parentToChildren: Map[RunId, Set[RunId]], childToParent: Map[RunId, RunId]) {
  def add(parentRunId: RunId, childRunId: RunId): CommandManagerState = {
    copy(
      parentToChildren = this.parentToChildren + (parentRunId → (this.parentToChildren(parentRunId) + childRunId)),
      childToParent = this.childToParent + (childRunId        → parentRunId)
    )
  }

  def remove(parentRunId: RunId, childRunID: RunId): CommandManagerState = {
    copy(
      parentToChildren = this.parentToChildren + (parentRunId → (this.parentToChildren(parentRunId) - childRunID)),
      childToParent = this.childToParent - childRunID
    )
  }

}
