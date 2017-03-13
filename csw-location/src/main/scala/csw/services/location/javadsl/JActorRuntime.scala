package csw.services.location.javadsl

import java.util

import csw.services.location.common.ActorRuntime
import csw.services.location.models.IActorRuntime
import scala.collection.JavaConverters._

object JActorRuntime extends IActorRuntime{
  override def create(name: String, settings: util.Map[String, AnyRef]): ActorRuntime = new ActorRuntime(name, settings.asScala.toMap)

  override def create(name: String): ActorRuntime = new ActorRuntime(name)
}
