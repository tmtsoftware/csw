package csw.services.config.server

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{HttpApp, Route}
import csw.services.config.scaladsl.ConfigManager

class ConfigServiceApp(configManager: ConfigManager, actorSystem: ActorSystem) extends HttpApp with HttpSupport {

  import actorSystem.dispatcher

  override protected def route: Route = {
    get {
      path("get") {
        (pathParam & idParam) { (filePath, maybeConfigId) ⇒
          rejectEmptyResponse & complete {
            configManager.get(filePath, maybeConfigId)
          }
        }
      } ~
        path("getDefault") {
          pathParam { filePath ⇒
            rejectEmptyResponse & complete {
              configManager.getDefault(filePath)
            }
          }
        } ~
        path("exists") {
          pathParam { filePath ⇒
            rejectEmptyResponse & complete {
              configManager.exists(filePath).map { found ⇒
                if (found) Some(Done) else None
              }
            }
          }
        } ~
        path("list") {
          complete(configManager.list())
        } ~
        path("history") {
          (pathParam & maxResultsParam) { (filePath, maxCount) ⇒
            complete(configManager.history(filePath, maxCount))
          }
        }
    } ~
      post {
        path("create") {
          (pathParam & fileDataParam & oversizeParam & commentParam) { (filePath, configSource, oversize, comment) ⇒
            complete(configManager.create(filePath, configSource, oversize, comment))
          }
        } ~
          path("update") {
            (pathParam & fileDataParam & commentParam) { (filePath, configSource, comment) ⇒
              complete(configManager.update(filePath, configSource, comment))
            }
          } ~
          path("setDefault") {
            (pathParam & idParam) { (filePath, maybeConfigId) ⇒
              complete(configManager.setDefault(filePath, maybeConfigId).map(_ ⇒ Done))
            }
          } ~
          path("resetDefault") {
            pathParam { filePath ⇒
              complete(configManager.resetDefault(filePath).map(_ ⇒ Done))
            }
          }
      }
  }
}
