package csw.time.api.models

trait Cancellable {
  def cancel(): Boolean
}
