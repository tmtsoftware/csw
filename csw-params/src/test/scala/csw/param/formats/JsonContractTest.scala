package csw.param.formats

import csw.param.commands.{CommandInfo, Setup}
import csw.param.generics.KeyType
import csw.param.models.{ObsId, Prefix, RaDec}
import org.scalatest.FunSpec

class JsonContractTest extends FunSpec {

  private val ck          = "wfos.blue.filter"
  private val commandInfo = CommandInfo(ObsId("Obs001"))

  describe("Test Custom RaDecItem") {
    it("Should allow custom RaDecItem") {
      val k1  = KeyType.RaDecKey.make("coords")
      val c1  = RaDec(7.3, 12.1)
      val c2  = RaDec(9.1, 2.9)
      val i1  = k1.set(c1, c2)
      val sc1 = Setup(commandInfo, Prefix(ck)).add(i1)

      val sc1out = JsonSupport.writeSequenceCommand(sc1)

      val sc1in = JsonSupport.readSequenceCommand[Setup](sc1out)
      assert(sc1.equals(sc1in))

    }
  }

}
