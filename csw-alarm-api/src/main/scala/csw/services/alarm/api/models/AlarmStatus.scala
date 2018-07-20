package csw.services.alarm.api.models

import csw.services.alarm.api.models.ActivationStatus.Active

case class AlarmStatus(
    acknowledgementStatus: AcknowledgementStatus,
    latchStatus: LatchStatus,
    latchedSeverity: AlarmSeverity,
    shelveStatus: ShelveStatus,
    activationStatus: ActivationStatus
) {
  def isActive: Boolean = activationStatus == Active
}
