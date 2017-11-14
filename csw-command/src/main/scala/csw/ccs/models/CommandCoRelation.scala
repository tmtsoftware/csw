package csw.ccs.models

import csw.messages.params.models.RunId

case class CommandCoRelation(parentToChildren: Map[RunId, Set[RunId]], childToParent: Map[RunId, RunId]) {
  def add(parentRunId: RunId, childRunId: RunId): CommandCoRelation = CommandCoRelation(
    parentToChildren.updated(parentRunId, parentToChildren.getOrElse(parentRunId, Set()) + childRunId),
    childToParent.updated(childRunId, parentRunId)
  )

  def remove(parentRunId: RunId, childRunId: RunId): CommandCoRelation = CommandCoRelation(
    parentToChildren.updated(parentRunId, parentToChildren.getOrElse(parentRunId, Set(childRunId)) - childRunId),
    childToParent - childRunId
  )

  def getParent(childRunId: RunId): Option[RunId] = childToParent.get(childRunId)

  def hasChildren(parentRunId: RunId): Boolean = parentToChildren.get(parentRunId) match {
    case Some(children) => children.nonEmpty
    case None           => false
  }
}
