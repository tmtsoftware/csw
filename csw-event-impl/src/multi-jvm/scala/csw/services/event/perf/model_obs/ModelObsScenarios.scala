package csw.services.event.perf.model_obs

import csw.messages.params.models.Prefix
import csw.messages.params.models.Subsystem.{AOESW, IRIS, NFIRAOS, TCS, WFOS}
import csw.services.event.perf.model_obs.BaseSetting.{PubSetting, SubSetting}
import csw.services.event.perf.wiring.TestConfigs

class ModelObsScenarios(testConfigs: TestConfigs) {
  import testConfigs._

  def adjustedTotalMsgs(n: Long): Long = (n * totalMessagesFactor).toLong

  private val tcs: String     = TCS.entryName
  private val wfos: String    = WFOS.entryName
  private val iris: String    = IRIS.entryName
  private val aoesw: String   = AOESW.entryName
  private val nfiraos: String = NFIRAOS.entryName

  // DEOPSCSW-405: [Redis]Measure performance of model observatory scenario
  // DEOPSCSW-406: [Kafka]Measure performance of model observatory scenario
  val idealMultiNodeModelObsScenario: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        ),
        List(
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        )
      ) ::
      List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem ⇒
        val subsystemName = subsystem.entryName

        (1 to 5).map {
          n ⇒
            JvmSetting(
              subsystemName,
              List(
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
              ),
              subsystem match {
                case AOESW ⇒
                  List(
                    SubSetting(Prefix(s"$iris-$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$iris-$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                  )
                case IRIS ⇒
                  List(
                    //              SubSetting(Prefix(s"$tcs-1"), noOfSubs = 1, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                  )
                case NFIRAOS ⇒
                  List(
                    //              SubSetting(Prefix(s"$tcs-1"), noOfSubs = 1, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$wfos-$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$wfos-$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                  )
                case WFOS ⇒
                  List(
                    //              SubSetting(Prefix(s"$tcs-1"), noOfSubs = 3, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$nfiraos-$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$nfiraos-$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                  )
                case _ ⇒ Nil
              }
            )
        }
      }
    )

  //DEOPSCSW-336: Pattern based subscription analysis and performance testing
  val idealMultiNodePatternModelObsScenario: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        ),
        List(
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 2, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        )
      ) ::
      List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem ⇒
        val subsystemName = subsystem.entryName

        (1 to 5).map {
          n ⇒
            JvmSetting(
              subsystemName,
              List(
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
              ),
              subsystem match {
                case AOESW ⇒
                  List(
                    SubSetting(Prefix(s"$iris-$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$iris-$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128),
                    SubSetting(Prefix(s"$iris-pattern-$n"), noOfSubs = 3, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                  )
                case IRIS ⇒
                  List(
                    //              SubSetting(Prefix(s"$tcs-1"), noOfSubs = 1, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-pattern-$n"), noOfSubs = 3, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                  )
                case NFIRAOS ⇒
                  List(
                    //              SubSetting(Prefix(s"$tcs-1"), noOfSubs = 1, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$wfos-$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$wfos-$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128),
                    SubSetting(Prefix(s"$wfos-pattern-$n"), noOfSubs = 2, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                  )
                case WFOS ⇒
                  List(
                    //              SubSetting(Prefix(s"$tcs-1"), noOfSubs = 3, adjustedTotalMessages(6000), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$nfiraos-$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$nfiraos-pattern-$n"), noOfSubs = 2, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                  )
                case _ ⇒ Nil
              }
            )
        }
      }
    )

  val modelObsScenarioWithFiveProcesses: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
        ),
        List(
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
        )
      ) ::
      List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem ⇒
        val subsystemName = subsystem.entryName

        (1 to 1).map {
          n ⇒
            JvmSetting(
              subsystemName,
              List(
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
              ),
              subsystem match {
                case AOESW ⇒
                  List(
                    SubSetting(Prefix(s"$iris-$n"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$iris-$n"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                  )
                case IRIS ⇒
                  List(
                    SubSetting(Prefix(s"$tcs-1"), noOfSubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                  )
                case NFIRAOS ⇒
                  List(
                    SubSetting(Prefix(s"$tcs-1"), noOfSubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$wfos-$n"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$wfos-$n"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                  )
                case WFOS ⇒
                  List(
                    SubSetting(Prefix(s"$tcs-1"), noOfSubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
                    SubSetting(Prefix(s"$nfiraos-$n"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$nfiraos-$n"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                  )
                case _ ⇒ Nil
              }
            )
        }
      }
    )

  val modelObsScenarioWithTwoProcesses: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
        ),
        List(
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
        )
      ) ::
      List(AOESW).flatMap { subsystem ⇒
        val subsystemName = subsystem.entryName

        (1 to 1).map {
          n ⇒
            JvmSetting(
              subsystemName,
              List(
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
              ),
              subsystem match {
                case AOESW ⇒
                  List(
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
                  )
                case _ ⇒ Nil
              }
            )
        }
      }
    )

  val modelObsScenarioPatternWithTwoProcesses: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(Prefix(s"$tcs-1"), noOfPubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
        ),
        List(
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-1"), noOfSubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128),
          SubSetting(Prefix(s"$tcs-pattern-1"), noOfSubs = 1, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
        )
      ) ::
      List(AOESW).flatMap { subsystem ⇒
        val subsystemName = subsystem.entryName

        (1 to 1).map {
          n ⇒
            JvmSetting(
              subsystemName,
              List(
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                PubSetting(Prefix(s"$subsystemName-$n"), noOfPubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
              ),
              subsystem match {
                case AOESW ⇒
                  List(
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-$n"), noOfSubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128),
                    SubSetting(Prefix(s"$aoesw-pattern-$n"), noOfSubs = 1, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
                  )
                case _ ⇒ Nil
              }
            )
        }
      }
    )

}
