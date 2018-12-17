package csw.time.api
import java.time.Duration

import csw.time.api.models.Cancellable

trait TimeService {
  def scheduleOnce(startTime: TAITime)(task: Runnable): Cancellable
  def schedulePeriodically(duration: Duration)(task: Runnable): Cancellable
  def schedulePeriodically(startTime: TAITime, duration: Duration)(task: Runnable): Cancellable
}
