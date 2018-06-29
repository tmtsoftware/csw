package csw.services.location.models

import akka.Done
import csw.messages.location.ActorSystemDependentFormats

trait UpickleFormats extends ActorSystemDependentFormats {
  import upickle.default.{macroRW, ReadWriter => RW, _}
  implicit val tcpRegistrationRw: RW[TcpRegistration]   = macroRW
  implicit val httpRegistrationRw: RW[HttpRegistration] = macroRW
  implicit val akkaRegistrationRw: RW[AkkaRegistration] = macroRW
  implicit val registrationRw: RW[Registration] =
    RW.merge(tcpRegistrationRw, httpRegistrationRw, akkaRegistrationRw)

  implicit val doneRW: RW[Done] = readwriter[String].bimap[Done](_ => "done", _ => Done)
}
