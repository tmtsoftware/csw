package csw.ccs.models

import csw.messages.params.models.RunId

case class CommandManagerState(parentToChildren: Map[RunId, Set[RunId]], childToParent: Map[RunId, RunId]) {
  def add(parentRunId: RunId, childRunId: RunId): CommandManagerState = {
    CommandManagerState(
      parentToChildren.updated(parentRunId, parentToChildren.getOrElse(parentRunId, Set()) + childRunId),
      childToParent.updated(childRunId, parentRunId)
    )
  }

  def remove(parentRunId: RunId, childRunID: RunId): CommandManagerState = {
    CommandManagerState(
      parentToChildren.updated(parentRunId, parentToChildren(parentRunId) - childRunID),
      childToParent - childRunID
    )
  }

}
