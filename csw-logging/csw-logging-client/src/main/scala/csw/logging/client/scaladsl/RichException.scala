package csw.logging.client.scaladsl

import csw.logging.api.NoLogException
import csw.logging.client.internal.JsonExtensions.AnyToJson

/**
 * The common parent of all rich exceptions.
 *
 * @param richMsg the rich exception message
 * @param cause the optional underlying causing exception
 */
case class RichException(richMsg: Any, cause: Throwable = NoLogException) extends Exception(richMsg.asJson.toString())
