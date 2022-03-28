/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.extensions

import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.internal.JAlarmServiceImpl

object AlarmServiceExt {

  implicit class RichAlarmService(val alarmService: AlarmService) {
    def asJava: JAlarmServiceImpl = new JAlarmServiceImpl(alarmService)
  }

}
