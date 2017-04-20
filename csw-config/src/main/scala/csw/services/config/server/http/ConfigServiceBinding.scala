package csw.services.config.server.http

import akka.http.scaladsl.Http.ServerBinding
import csw.services.location.models.RegistrationResult

case class ConfigServiceBinding(serverBinding: ServerBinding, registrationResult: RegistrationResult)
