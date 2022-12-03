/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.perf.model_obs

import csw.event.client.perf.model_obs.BaseSetting.{PubSetting, SubSetting}
import csw.event.client.perf.wiring.TestConfigs
import csw.prefix.models.Subsystem._
import csw.prefix.models.Prefix

class ModelObsScenarios(testConfigs: TestConfigs) {
  import testConfigs._

  def adjustedTotalMsgs(n: Long): Long = (n * totalMessagesFactor).toLong

  private val tcs: String     = TCS.entryName
  private val wfos: String    = WFOS.entryName
  private val iris: String    = IRIS.entryName
  private val aoesw: String   = AOESW.entryName
  private val nfiraos: String = NFIRAOS.entryName
  private val dms: String     = DMS.entryName

  private val peakLoadSettings: List[JvmSetting] = (1 to 4).map { n =>
    JvmSetting(
      dms,
      List(PubSetting(Prefix(s"$dms.$n"), noOfPubs = 5, adjustedTotalMsgs(1000), rate = 1000, payloadSize = 64)),
      List(SubSetting(Prefix(s"$dms.$n"), noOfSubs = 5, adjustedTotalMsgs(1000), rate = 1000, payloadSize = 64))
    )
  }.toList

  private val tcs1Prefix = Prefix(s"$tcs.1")

  /**
   * Don not run below scenarios on local box, as it generates load and requires multiple machines to support.
   * For Local testing, try runnig scenarios mentioned at the bottom of this file.
   * */
  // DEOPSCSW-405: [Redis]Measure performance of model observatory scenario
  // DEOPSCSW-406: [Kafka]Measure performance of model observatory scenario
  val idealMultiNodeModelObsScenario: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(tcs1Prefix, noOfPubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        ),
        List(
          SubSetting(tcs1Prefix, noOfSubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        )
      ) ::
        List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem =>
          val subsystemName = subsystem.entryName

          (1 to 5).map {
            n =>
              JvmSetting(
                subsystemName,
                List(
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                ),
                List(
                  SubSetting(Prefix(s"$subsystemName.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  SubSetting(Prefix(s"$subsystemName.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                )
              )
          }
        }
    )

  /**
   * Don not run below scenarios on local box, as it generates load and requires multiple machines to support.
   * For Local testing, try runnig scenarios mentioned at the bottom of this file.
   * */
  // DEOPSCSW-405: [Redis]Measure performance of model observatory scenario
  // DEOPSCSW-406: [Kafka]Measure performance of model observatory scenario
  val idealMultiNodeModelObsScenarioAfterAwsNtpClockSync: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(tcs1Prefix, noOfPubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        ),
        List(
          SubSetting(tcs1Prefix, noOfSubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        )
      ) ::
        List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem =>
          val subsystemName = subsystem.entryName

          (1 to 5).map {
            n =>
              JvmSetting(
                subsystemName,
                List(
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                ),
                subsystem match {
                  case AOESW =>
                    List(
                      SubSetting(Prefix(s"$iris.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$iris.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                    )
                  case IRIS =>
                    List(
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                    )
                  case NFIRAOS =>
                    List(
                      SubSetting(Prefix(s"$wfos.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$wfos.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                    )
                  case WFOS =>
                    List(
                      SubSetting(Prefix(s"$nfiraos.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$nfiraos.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                    )
                  case _ => Nil
                }
              )
          }
        }
    )

  // DEOPSCSW-336: Pattern based subscription analysis and performance testing
  // DEOPSCSW-420: Implement Pattern based subscription
  val idealMultiNodePatternModelObsScenario: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(tcs1Prefix, noOfPubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        ),
        List(
          SubSetting(tcs1Prefix, noOfSubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 2, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        )
      ) ::
        List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem =>
          val subsystemName = subsystem.entryName

          (1 to 5).map {
            n =>
              JvmSetting(
                subsystemName,
                List(
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                ),
                subsystem match {
                  case AOESW =>
                    List(
                      SubSetting(Prefix(s"$iris.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$iris.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128),
                      SubSetting(Prefix(s"$iris.pattern.$n"), noOfSubs = 3, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                    )
                  case IRIS =>
                    List(
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128),
                      SubSetting(Prefix(s"$aoesw.pattern.$n"), noOfSubs = 3, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                    )
                  case NFIRAOS =>
                    List(
                      SubSetting(Prefix(s"$wfos.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$wfos.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128),
                      SubSetting(Prefix(s"$wfos.pattern.$n"), noOfSubs = 2, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                    )
                  case WFOS =>
                    List(
                      SubSetting(Prefix(s"$nfiraos.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$nfiraos.pattern.$n"), noOfSubs = 2, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                    )
                  case _ => Nil
                }
              )
          }
        }
    )

  // DEOPSCSW-362: Support publication of 20, 64byte events at 1Khz
  val idealMultiNodeModelObsWithPeakLoadOf20K =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(tcs1Prefix, noOfPubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        ),
        List(
          SubSetting(tcs1Prefix, noOfSubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 250, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
        )
      ) ::
        List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem =>
          val subsystemName = subsystem.entryName

          (1 to 5).map {
            n =>
              JvmSetting(
                subsystemName,
                List(
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                ),
                List(
                  SubSetting(Prefix(s"$subsystemName.$n"), noOfSubs = 5, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  SubSetting(Prefix(s"$subsystemName.$n"), noOfSubs = 50, adjustedTotalMsgs(1), rate = 1, payloadSize = 128)
                )
              )
          }
        } :::
        peakLoadSettings
    )

  /**
   * Scenarios for running on local box
   * */
  val modelObsScenarioWithFiveProcesses: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(tcs1Prefix, noOfPubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
        ),
        List(
          SubSetting(tcs1Prefix, noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
        )
      ) ::
        List(AOESW, IRIS, NFIRAOS, WFOS).flatMap { subsystem =>
          val subsystemName = subsystem.entryName

          (1 to 1).map {
            n =>
              JvmSetting(
                subsystemName,
                List(
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                ),
                subsystem match {
                  case AOESW =>
                    List(
                      SubSetting(Prefix(s"$iris.$n"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$iris.$n"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                    )
                  case IRIS =>
                    List(
                      SubSetting(tcs1Prefix, noOfSubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                    )
                  case NFIRAOS =>
                    List(
                      SubSetting(tcs1Prefix, noOfSubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
                      SubSetting(Prefix(s"$wfos.$n"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$wfos.$n"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                    )
                  case WFOS =>
                    List(
                      SubSetting(tcs1Prefix, noOfSubs = 3, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
                      SubSetting(Prefix(s"$nfiraos.$n"), noOfSubs = 25, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$nfiraos.$n"), noOfSubs = 100, adjustedTotalMsgs(3), rate = 3, payloadSize = 128)
                    )
                  case _ => Nil
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
          PubSetting(tcs1Prefix, noOfPubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
        ),
        List(
          SubSetting(tcs1Prefix, noOfSubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
        )
      ) ::
        List(AOESW).flatMap { subsystem =>
          val subsystemName = subsystem.entryName

          (1 to 1).map {
            n =>
              JvmSetting(
                subsystemName,
                List(
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
                ),
                subsystem match {
                  case AOESW =>
                    List(
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
                    )
                }
              )
          }
        }
    )

  // DEOPSCSW-336: Pattern based subscription analysis and performance testing
  // DEOPSCSW-420: Implement Pattern based subscription
  val modelObsScenarioPatternWithTwoProcesses: ModelObservatoryTestSettings =
    ModelObservatoryTestSettings(
      JvmSetting(
        tcs,
        List(
          PubSetting(tcs1Prefix, noOfPubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          PubSetting(tcs1Prefix, noOfPubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
        ),
        List(
          SubSetting(tcs1Prefix, noOfSubs = 1, adjustedTotalMsgs(100), rate = 100, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
          SubSetting(tcs1Prefix, noOfSubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128),
          SubSetting(Prefix(s"$tcs.pattern.1"), noOfSubs = 1, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
        )
      ) ::
        List(AOESW).flatMap { subsystem =>
          val subsystemName = subsystem.entryName

          (1 to 1).map {
            n =>
              JvmSetting(
                subsystemName,
                List(
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                  PubSetting(Prefix(s"$subsystemName.$n"), noOfPubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
                ),
                subsystem match {
                  case AOESW =>
                    List(
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 10, adjustedTotalMsgs(20), rate = 20, payloadSize = 128),
                      SubSetting(Prefix(s"$aoesw.$n"), noOfSubs = 30, adjustedTotalMsgs(5), rate = 5, payloadSize = 128),
                      SubSetting(Prefix(s"$aoesw.pattern.$n"), noOfSubs = 1, adjustedTotalMsgs(5), rate = 5, payloadSize = 128)
                    )
                }
              )
          }
        }
    )

}
