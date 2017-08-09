package csw.param.formats

import java.time.Instant

import csw.param.commands.{CommandInfo, Observe, Setup, Wait}
import csw.param.events._
import csw.param.generics.KeyType
import csw.param.generics.KeyType.{LongMatrixKey, StructKey}
import csw.param.models._
import org.scalatest.{FunSpec, Matchers}
import spray.json.pimpString

class JsonContractTest extends FunSpec with Matchers {

  private val prefixStr: String        = "wfos.blue.filter"
  private val prefix: Prefix           = Prefix(prefixStr)
  private val runId: RunId             = RunId("f22dc990-a02c-4d7e-b719-50b167cb7a1e")
  private val obsId: ObsId             = ObsId("Obs001")
  private val commandInfo: CommandInfo = CommandInfo(obsId, runId)
  private val instantStr: String       = "2017-08-09T06:40:00.898Z"
  private val eventId: String          = "7a4cd6ab-6077-476d-a035-6f83be1de42c"
  private val eventTime: EventTime     = EventTime(Instant.parse(instantStr))
  private val eventInfo: EventInfo     = EventInfo(prefix, eventTime, Some(obsId), eventId)

  private val setupJsonExample = """{
                                   |  "type": "Setup",
                                   |  "info": {
                                   |    "obsId": "Obs001",
                                   |    "runId": "f22dc990-a02c-4d7e-b719-50b167cb7a1e"
                                   |  },
                                   |  "prefix": {
                                   |    "subsystem": "wfos",
                                   |    "prefix": "wfos.blue.filter"
                                   |  },
                                   |  "paramSet": [{
                                   |    "keyName": "coords",
                                   |    "keyType": "RaDecKey",
                                   |    "values": [{
                                   |      "ra": 7.3,
                                   |      "dec": 12.1
                                   |    }, {
                                   |      "ra": 9.1,
                                   |      "dec": 2.9
                                   |    }],
                                   |    "units": "[none]"
                                   |  }]
                                   |}
                                   |""".stripMargin

  val observeJsonExample = """{
                             |  "type": "Observe",
                             |  "info": {
                             |    "obsId": "Obs001",
                             |    "runId": "f22dc990-a02c-4d7e-b719-50b167cb7a1e"
                             |  },
                             |  "prefix": {
                             |    "subsystem": "wfos",
                             |    "prefix": "wfos.blue.filter"
                             |  },
                             |  "paramSet": [{
                             |    "keyName": "repeat",
                             |    "keyType": "IntKey",
                             |    "values": [22],
                             |    "units": "[none]"
                             |  }, {
                             |    "keyName": "expTime",
                             |    "keyType": "StringKey",
                             |    "values": ["11:10"],
                             |    "units": "[none]"
                             |  }]
                             |}""".stripMargin

  val waitJsonExample = """{
                          |  "type": "Wait",
                          |  "info": {
                          |    "obsId": "Obs001",
                          |    "runId": "f22dc990-a02c-4d7e-b719-50b167cb7a1e"
                          |  },
                          |  "prefix": {
                          |    "subsystem": "wfos",
                          |    "prefix": "wfos.blue.filter"
                          |  },
                          |  "paramSet": [{
                          |    "keyName": "myMatrix",
                          |    "keyType": "LongMatrixKey",
                          |    "values": [{
                          |      "data": [[1, 2, 3], [2, 3, 6], [4, 6, 12]]
                          |    }, {
                          |      "data": [[2, 3, 4], [5, 6, 7], [8, 9, 10]]
                          |    }],
                          |    "units": "[none]"
                          |  }]
                          |}""".stripMargin

  val statusEventJsonExample = s"""{
                                 |  "type": "StatusEvent",
                                 |  "info": {
                                 |    "source": {
                                 |      "subsystem": "wfos",
                                 |      "prefix": "${prefix.prefix}"
                                 |    },
                                 |    "eventTime": "$instantStr",
                                 |    "obsId":"${obsId.obsId}",
                                 |    "eventId": "$eventId"
                                 |  },
                                 |  "paramSet": [{
                                 |    "keyName": "encoder",
                                 |    "keyType": "IntKey",
                                 |    "values": [22],
                                 |    "units": "[none]"
                                 |  }, {
                                 |    "keyName": "windspeed",
                                 |    "keyType": "IntKey",
                                 |    "values": [44],
                                 |    "units": "[none]"
                                 |  }]
                                 |}""".stripMargin

  val observeEventJsonExample = """{
                                  |  "type": "ObserveEvent",
                                  |  "info": {
                                  |    "source": {
                                  |      "subsystem": "wfos",
                                  |      "prefix": "wfos.blue.filter"
                                  |    },
                                  |    "eventTime": "2017-08-09T06:40:00.898Z",
                                  |    "obsId": "Obs001",
                                  |    "eventId": "7a4cd6ab-6077-476d-a035-6f83be1de42c"
                                  |  },
                                  |  "paramSet": [{
                                  |    "keyName": "myStruct",
                                  |    "keyType": "StructKey",
                                  |    "values": [{
                                  |      "paramSet": [{
                                  |        "keyName": "ra",
                                  |        "keyType": "StringKey",
                                  |        "values": ["12:13:14.1"],
                                  |        "units": "[none]"
                                  |      }, {
                                  |        "keyName": "dec",
                                  |        "keyType": "StringKey",
                                  |        "values": ["32:33:34.4"],
                                  |        "units": "[none]"
                                  |      }, {
                                  |        "keyName": "epoch",
                                  |        "keyType": "DoubleKey",
                                  |        "values": [1950.0],
                                  |        "units": "[none]"
                                  |      }]
                                  |    }],
                                  |    "units": "[none]"
                                  |  }]
                                  |}""".stripMargin

  val systemEventJsonExample = s"""{
                                 |  "type": "SystemEvent",
                                 |  "info": {
                                 |    "source": {
                                 |      "subsystem": "wfos",
                                 |      "prefix": "${prefix.prefix}"
                                 |    },
                                 |    "eventTime": "$instantStr",
                                 |    "obsId": "${obsId.obsId}",
                                 |    "eventId": "$eventId"
                                 |  },
                                 |  "paramSet": [{
                                 |    "keyName": "arrayDataKey",
                                 |    "keyType": "ByteArrayKey",
                                 |    "values": [{
                                 |      "data": [1, 2, 3, 4, 5]
                                 |    }, {
                                 |      "data": [10, 20, 30, 40, 50]
                                 |    }],
                                 |    "units": "[none]"
                                 |  }]
                                 |}""".stripMargin

  describe("Test Sequence Commands") {

    it("Should adhere to specified standard Setup json format") {
      val raDecKey   = KeyType.RaDecKey.make("coords")
      val raDec1     = RaDec(7.3, 12.1)
      val raDec2     = RaDec(9.1, 2.9)
      val raDecParam = raDecKey.set(raDec1, raDec2)

      val setup       = Setup(commandInfo, prefix).add(raDecParam)
      val setupToJson = JsonSupport.writeSequenceCommand(setup)

      assert(setupToJson.equals(setupJsonExample.parseJson))
    }

    it("Should adhere to specified standard Observe json format") {
      val k1      = KeyType.IntKey.make("repeat")
      val k2      = KeyType.StringKey.make("expTime")
      val i1      = k1.set(22)
      val i2      = k2.set("11:10")
      val observe = Observe(commandInfo, prefix).add(i1).add(i2)

      val observeToJson = JsonSupport.writeSequenceCommand(observe)

      assert(observeToJson.equals(observeJsonExample.parseJson))
    }

    it("Should adhere to specified standard Wait json format") {
      val k1                   = LongMatrixKey.make("myMatrix")
      val m1: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12))
      val m2: MatrixData[Long] = MatrixData.fromArrays(Array(2, 3, 4), Array(5, 6, 7), Array(8, 9, 10))
      val matrixParam          = k1.set(m1, m2)

      val wait       = Wait(commandInfo, prefix).add(matrixParam)
      val waitToJson = JsonSupport.writeSequenceCommand(wait)

      assert(waitToJson.equals(waitJsonExample.parseJson))
    }
  }

  describe("Test Events") {

    it("Should adhere to specified standard StatusEvent json format") {
      val k1 = KeyType.IntKey.make("encoder")
      val k2 = KeyType.IntKey.make("windspeed")

      val i1                = k1.set(22)
      val i2                = k2.set(44)
      val statusEvent       = StatusEvent(eventInfo).madd(i1, i2)
      val statusEventToJson = JsonSupport.writeEvent(statusEvent)

      statusEventToJson shouldEqual statusEventJsonExample.parseJson
    }

    it("Should adhere to specified standard ObserveEvent json format") {
      val structKey = StructKey.make("myStruct")

      val ra         = KeyType.StringKey.make("ra")
      val dec        = KeyType.StringKey.make("dec")
      val epoch      = KeyType.DoubleKey.make("epoch")
      val structItem = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val structParam        = structKey.set(structItem)
      val observeEvent       = ObserveEvent(eventInfo).add(structParam)
      val observeEventToJson = JsonSupport.writeEvent(observeEvent)

      observeEventToJson shouldEqual observeEventJsonExample.parseJson
    }

    it("Should adhere to specified standard SystemEvent json format") {
      val a1: Array[Byte] = Array[Byte](1, 2, 3, 4, 5)
      val a2: Array[Byte] = Array[Byte](10, 20, 30, 40, 50)

      val arrayDataKey   = KeyType.ByteArrayKey.make("arrayDataKey")
      val arrayDataParam = arrayDataKey.set(ArrayData(a1), ArrayData(a2))

      val systemEvent       = SystemEvent(eventInfo).add(arrayDataParam)
      val systemEventToJson = JsonSupport.writeEvent(systemEvent)

      systemEventToJson shouldEqual systemEventJsonExample.parseJson
    }
  }

}
