package csw.services.alarm.api.scaladsl

import scala.concurrent.Future

trait AlarmSubscription {

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Unit]
}
