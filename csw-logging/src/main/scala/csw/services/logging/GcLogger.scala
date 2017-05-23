package csw.services.logging

import java.lang.management.MemoryUsage
import javax.management.openmbean.CompositeData
import javax.management.{Notification, NotificationEmitter, NotificationListener}

import com.persist.JsonOps._
import com.sun.management.GarbageCollectionNotificationInfo

import scala.collection.JavaConverters._

private[logging] case class GcLogger(log: Logger) {
  private[this] val gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans
  private[this] val emitters = for (gcbean <- gcbeans.asScala) yield {
    val emitter = gcbean.asInstanceOf[NotificationEmitter]
    val listener = new NotificationListener() {
      @Override
      def handleNotification(notification: Notification, handback: Object): Unit =
        if (notification.getType.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
          val info     = GarbageCollectionNotificationInfo.from(notification.getUserData.asInstanceOf[CompositeData])
          val duration = info.getGcInfo.getDuration // microseconds
          val gcaction = info.getGcAction
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
            "id"       -> info.getGcInfo.getId,
            "name"     -> info.getGcName,
            "cause"    -> info.getGcCause,
            "start"    -> info.getGcInfo.getStartTime,
            "end"      -> info.getGcInfo.getEndTime,
            "before"   -> getMem(info.getGcInfo.getMemoryUsageBeforeGc.asScala.toMap[String, MemoryUsage]),
            "after"    -> getMem(info.getGcInfo.getMemoryUsageAfterGc.asScala.toMap[String, MemoryUsage]),
            "duration" -> duration
          )

          log.alternative("gc", data)
        }
    }
    emitter.addNotificationListener(listener, null, null)
    (emitter, listener)
  }

  def stop(): Unit =
    for ((e, l) <- emitters) {
      e.removeNotificationListener(l)
    }
}
