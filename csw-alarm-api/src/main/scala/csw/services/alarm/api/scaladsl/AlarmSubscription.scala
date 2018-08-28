package csw.services.alarm.api.scaladsl

import akka.Done

import scala.concurrent.Future

trait AlarmSubscription {

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Done]

  /**
   * To check if the underlying subscription is ready to emit elements
   * @return a future which completes when the underlying subscription is ready to emit elements
   */
  def ready(): Future[Done]
}
