package csw.services.config.server

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.server.{HttpApp, Route}
import csw.services.config.internal.JsonSupport
import csw.services.config.models.{ConfigId, ConfigSource}
import csw.services.config.scaladsl.ConfigManager

class ConfigServiceApp(configManager: ConfigManager) extends HttpApp with JsonSupport {

  private val actorSystem = ActorSystem()

  import actorSystem.dispatcher

  override protected def route: Route = {
    get {
      path("get") {
        parameters('path, 'id.?) { (filePath, id) ⇒
          rejectEmptyResponse {
            complete {
              configManager.get(Paths.get(filePath).toFile, id.map(ConfigId.apply)).map { maybeConfigData ⇒
                maybeConfigData.map(configData ⇒ Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source))
              }
            }
          }
        }
      } ~
        path("exists") {
          parameter('path) { filePath ⇒
            rejectEmptyResponse {
              complete {
                configManager.exists(Paths.get(filePath).toFile).map { found ⇒
                  if (found) Some("ok") else None
                }
              }
            }
          }
        } ~
        path("list") {
          complete(configManager.list())
        } ~
        path("history") {
          parameters('path, 'maxResults.as[Int] ? 100) { (filePath, maxCount) ⇒
            complete(configManager.history(Paths.get(filePath).toFile, maxCount))
          }
        } ~
        path("getDefault") {
          parameters('path) { filePath ⇒
            rejectEmptyResponse {
              complete {
                configManager.getDefault(Paths.get(filePath).toFile).map { maybeConfigData ⇒
                  maybeConfigData.map(configData ⇒ Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source))
                }
              }
            }
          }
        }
    } ~
      post {
        path("setDefault") {
          parameters('path, 'id.?) { (filePath, id) ⇒
            complete(configManager.setDefault(Paths.get(filePath).toFile, id.map(ConfigId.apply)).map(_ ⇒ "ok"))
          }
        } ~
          path("resetDefault") {
            parameters('path) { filePath ⇒
              complete(configManager.resetDefault(Paths.get(filePath).toFile).map(_ ⇒ "ok"))
            }
          } ~
          parameters('path, 'comment ? "") { (filePath, comment) ⇒
            fileUpload("conf") { case (fileInfo, source) ⇒
              path("create") {
                parameter('oversize.as[Boolean] ? false) { oversize ⇒
                  val eventualId = configManager.create(Paths.get(filePath).toFile, ConfigSource(source), oversize, comment)
                  complete(eventualId.map(_.id))
                }
              } ~
                path("update") {
                  val eventualId = configManager.update(Paths.get(filePath).toFile, ConfigSource(source), comment)
                  complete(eventualId.map(_.id))
                }
            }
          }
      }
  }
}
