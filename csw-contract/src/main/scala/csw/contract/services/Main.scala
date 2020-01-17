package csw.contract.services

import csw.contract.services.codecs.ContractCodecs
import csw.contract.services.data.ServiceData
import io.bullet.borer.Json
import play.api.libs.json

object Main extends ContractCodecs {

  def main(args: Array[String]): Unit = {
    val data         = Json.encode(ServiceData.data).toUtf8String
    val contractJson = json.Json.prettyPrint(json.Json.parse(data))
    print(contractJson)
  }
}
