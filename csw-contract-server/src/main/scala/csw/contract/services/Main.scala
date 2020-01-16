package csw.contract.services

import java.io.{File, PrintWriter}

import play.api.libs.json

object Main extends App {

  val location     = new File("csw-contract-server/src/main/scala/csw/contract/services/location/register.json")
  val print_Writer = new PrintWriter(location)
  print_Writer.write(json.Json.prettyPrint(Location.contractSamples))
  print_Writer.close()
}
