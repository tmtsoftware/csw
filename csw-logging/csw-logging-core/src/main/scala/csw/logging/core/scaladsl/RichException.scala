package csw.logging.core.scaladsl

import csw.logging.core.NoLogException
import csw.logging.core.internal.JsonExtensions.AnyToJson

/**
 * The common parent of all rich exceptions.
 *
 * @param richMsg the rich exception message
 * @param cause the optional underlying causing exception
 */
case class RichException(richMsg: Any, cause: Throwable = NoLogException) extends Exception(richMsg.asJson.toString())
