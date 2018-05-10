package csw.services.event.perf

import csw.messages.params.models.Subsystem.{AOESW, IRIS, NFIRAOS, TCS, WFOS}

class ModelObsScenarios(testConfigs: TestConfigs) {
  import testConfigs._

  def adjustedTotalMessages(n: Long): Long = (n * totalMessagesFactor).toLong

  val idealMultiNodeModelObsScenario: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        TCS.entryName,
        List(
          PubSetting(s"${TCS.entryName}-1", noOfPubs = 3, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
          PubSetting(s"${TCS.entryName}-1", noOfPubs = 25, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
          PubSetting(s"${TCS.entryName}-1", noOfPubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
        ),
        List(
          SubSetting(s"${TCS.entryName}-1", noOfSubs = 3, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
          SubSetting(s"${TCS.entryName}-1", noOfSubs = 25, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
          SubSetting(s"${TCS.entryName}-1", noOfSubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
        )
      ) ::
      List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem ⇒
        val subsystemName = subsystem.entryName

        (1 to 5).map {
          n ⇒
            JvmSetting(
              subsystemName,
              List(
                PubSetting(s"$subsystemName-$n", noOfPubs = 5, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
                PubSetting(s"$subsystemName-$n", noOfPubs = 50, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
              ),
              subsystem match {
                case AOESW ⇒
                  List(
                    SubSetting(s"${IRIS.entryName}-$n", noOfSubs = 5, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
                    SubSetting(s"${IRIS.entryName}-$n", noOfSubs = 50, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
                  )
                case IRIS ⇒
                  List(
                    //              SubSetting(s"${TCS.entryName}-1", noOfSubs = 1, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(s"${AOESW.entryName}-$n", noOfSubs = 5, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
                    SubSetting(s"${AOESW.entryName}-$n", noOfSubs = 50, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
                  )
                case NFIRAOS ⇒
                  List(
                    //              SubSetting(s"${TCS.entryName}-1", noOfSubs = 1, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(s"${WFOS.entryName}-$n", noOfSubs = 5, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
                    SubSetting(s"${WFOS.entryName}-$n", noOfSubs = 50, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
                  )
                case WFOS ⇒
                  List(
                    //              SubSetting(s"${TCS.entryName}-1", noOfSubs = 3, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(s"${NFIRAOS.entryName}-$n",
                               noOfSubs = 5,
                               adjustedTotalMessages(1200),
                               rate = 20,
                               payloadSize = 128),
                    SubSetting(s"${NFIRAOS.entryName}-$n", noOfSubs = 50, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
                  )
              }
            )
        }
      }
    )

  val modelObsScenarioWithFiveProcesses: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        TCS.entryName,
        List(
          PubSetting(s"${TCS.entryName}-1", noOfPubs = 3, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
          PubSetting(s"${TCS.entryName}-1", noOfPubs = 25, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
          PubSetting(s"${TCS.entryName}-1", noOfPubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
        ),
        List(
          SubSetting(s"${TCS.entryName}-1", noOfSubs = 25, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
          SubSetting(s"${TCS.entryName}-1", noOfSubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
        )
      ) ::
      List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem ⇒
        val subsystemName = subsystem.entryName

        (1 to 1).map {
          n ⇒
            JvmSetting(
              subsystemName,
              List(
                PubSetting(s"$subsystemName-$n", noOfPubs = 25, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
                PubSetting(s"$subsystemName-$n", noOfPubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
              ),
              subsystem match {
                case AOESW ⇒
                  List(
                    SubSetting(s"${IRIS.entryName}-$n", noOfSubs = 25, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
                    SubSetting(s"${IRIS.entryName}-$n", noOfSubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
                  )
                case IRIS ⇒
                  List(
                    SubSetting(s"${TCS.entryName}-1", noOfSubs = 1, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(s"${AOESW.entryName}-$n",
                               noOfSubs = 25,
                               adjustedTotalMessages(1200),
                               rate = 20,
                               payloadSize = 128),
                    SubSetting(s"${AOESW.entryName}-$n", noOfSubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
                  )
                case NFIRAOS ⇒
                  List(
                    SubSetting(s"${TCS.entryName}-1", noOfSubs = 1, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(s"${WFOS.entryName}-$n", noOfSubs = 25, adjustedTotalMessages(1200), rate = 20, payloadSize = 128),
                    SubSetting(s"${WFOS.entryName}-$n", noOfSubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
                  )
                case WFOS ⇒
                  List(
                    SubSetting(s"${TCS.entryName}-1", noOfSubs = 3, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(s"${NFIRAOS.entryName}-$n",
                               noOfSubs = 25,
                               adjustedTotalMessages(1200),
                               rate = 20,
                               payloadSize = 128),
                    SubSetting(s"${NFIRAOS.entryName}-$n", noOfSubs = 250, adjustedTotalMessages(60), rate = 1, payloadSize = 128)
                  )
              }
            )
        }
      }
    )

}
