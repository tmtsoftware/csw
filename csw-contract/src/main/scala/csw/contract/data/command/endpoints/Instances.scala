package csw.contract.data.command.endpoints

import csw.command.api.codecs.CommandServiceCodecs
import csw.contract.generator.models.Endpoint

object Instances extends CommandServiceCodecs {

  val endpoints: List[Endpoint] = List(
//    "validate" -> Endpoint(
//      requests = List(observeValidate, setupValidate),
//      responses = List(accepted, invalid)
//    ),
//    "submit" -> Endpoint(
//      requests = List(observeSubmit, setupSubmit),
//      responses = List(accepted, cancelled)
//    ),
//    "oneWay" -> Endpoint(
//      requests = List(observeOneway, setupOneway),
//      responses = List(accepted, invalid)
//    ),
//    "query" -> Endpoint(
//      requests = List(setupQuery),
//      responses = List(accepted, cancelled)
//    ),
//    "queryFinal" -> Endpoint(
//      requests = List(queryFinal, setupValidate),
//      responses = List(started, accepted)
//    ),
//    "subscribeCurrentState" -> Endpoint(
//      requests = List(subscribeState),
//      responses = List(currentState)
//    )
  )
}
