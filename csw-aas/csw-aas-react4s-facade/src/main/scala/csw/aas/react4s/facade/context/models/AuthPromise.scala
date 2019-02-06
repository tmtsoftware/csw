package csw.aas.react4s.facade.context.models

import scala.scalajs.js

@js.native
trait AuthPromise[Success, Error] extends js.Object {

  type Callback[T] = js.Function1[T, Unit]

  /**
   * Function to call if the promised action succeeds.
   */
  def success(callback: Callback[Success]): AuthPromise[Success, Error] = js.native

  /**
   * Function to call if the promised action throws an error.
   */
  def error(callback: Callback[Error]): AuthPromise[Success, Error] = js.native
}
