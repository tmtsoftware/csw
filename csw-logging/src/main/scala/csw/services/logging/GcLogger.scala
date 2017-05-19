package csw.services.logging

import javax.management.{Notification, NotificationEmitter, NotificationListener}
import scala.collection.JavaConversions._
import com.sun.management.GarbageCollectionNotificationInfo
import javax.management.openmbean.CompositeData
import com.persist.JsonOps._
import java.lang.management.MemoryUsage

private[logging] case class GcLogger() extends ClassLogging {
  private[this] val gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
  private[this] val emitters = for (gcbean <- gcbeans) yield {
    val emitter = gcbean.asInstanceOf[NotificationEmitter]
    val listener = new NotificationListener() {
      @Override
      def handleNotification(notification: Notification, handback: Object) {
        if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
          val info     = GarbageCollectionNotificationInfo.from(notification.getUserData().asInstanceOf[CompositeData])
          val duration = info.getGcInfo().getDuration() // microseconds
          val gcaction = info.getGcAction()
          val gctype = if ("end of minor GC".equals(gcaction)) {
            "minor"
          } else if ("end of major GC".equals(gcaction)) {
            "major"
          } else {
            "unknown"
          }
          def getMem(mem: Map[String, MemoryUsage]): Json = {
            val m = mem map {
              case (name, usage) =>
                (name, JsonObject("used" -> usage.getUsed, "max" -> usage.getMax, "committed" -> usage.getCommitted))
            }
            m
          }
          val data = JsonObject(
            "type"     -> gctype,
            "id"       -> info.getGcInfo().getId(),
            "name"     -> info.getGcName(),
            "cause"    -> info.getGcCause(),
            "start"    -> info.getGcInfo().getStartTime,
            "end"      -> info.getGcInfo.getEndTime,
            "before"   -> getMem(info.getGcInfo.getMemoryUsageBeforeGc.toMap[String, MemoryUsage]),
            "after"    -> getMem(info.getGcInfo.getMemoryUsageAfterGc.toMap[String, MemoryUsage]),
            "duration" -> duration
          )

          log.alternative("gc", data)
        }
      }
    }
    emitter.addNotificationListener(listener, null, null)
    (emitter, listener)
  }

  def stop: Unit =
    for ((e, l) <- emitters) {
      e.removeNotificationListener(l)
    }
}
