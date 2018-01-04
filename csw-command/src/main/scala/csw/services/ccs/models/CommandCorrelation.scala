package csw.services.ccs.models

import csw.messages.params.models.RunId

/**
 * This model maintains the relation of a command that can be split into two or more commands for execution. The state of the parent command then depends on the
 * children commands.
 * @param parentToChildren a map of parent command to all children command
 * @param childToParent a map of a child command to its parent command
 */
case class CommandCorrelation(parentToChildren: Map[RunId, Set[RunId]], childToParent: Map[RunId, RunId]) {

  /**
   * Add a new command relation
   * @param parentRunId parent command identifier as RunID
   * @param childRunId child command identifier as RunID
   * @return a new CommandCorrelation model with te added coorelation
   */
  def add(parentRunId: RunId, childRunId: RunId): CommandCorrelation = CommandCorrelation(
    parentToChildren.updated(parentRunId, parentToChildren.getOrElse(parentRunId, Set()) + childRunId),
    childToParent.updated(childRunId, parentRunId)
  )

  /**
   * Remove a command relation
   * @param parentRunId parent command identifier as RunID
   * @param childRunId child command identifier as RunID
   * @return a new CommandCorrelation model with the correlation removed
   */
  def remove(parentRunId: RunId, childRunId: RunId): CommandCorrelation = CommandCorrelation(
    parentToChildren.updated(parentRunId, parentToChildren.getOrElse(parentRunId, Set(childRunId)) - childRunId),
    childToParent - childRunId
  )

  /**
   * Get parent command Identifier for a given command
   * @param childRunId command identifier as RunID
   * @return an option of command identifier as RunID
   */
  def getParent(childRunId: RunId): Option[RunId] = childToParent.get(childRunId)

  def hasChildren(parentRunId: RunId): Boolean = parentToChildren.get(parentRunId) match {
    case Some(children) => children.nonEmpty
    case None           => false
  }

}
