package csw.param.parameters

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import csw.param.{CurrentState, CurrentStates, DemandState, StateVariable}
import csw.param.events._
import csw.param.commands._

/**
 * Defines methods for serializing parameter sets
 */
object ParamSetSerializer {

  /**
   * Defines API to serialize to a byte array
   */
  trait ParamSetSerializer[A] {
    def write(in: A): Array[Byte]

    def read(bytes: Array[Byte]): A
  }

  def read[A](bytes: Array[Byte])(implicit cl: ParamSetSerializer[A]): A = cl.read(bytes)

  def write[A](in: A)(implicit cl: ParamSetSerializer[A]): Array[Byte] = cl.write(in)

  def writeObj[A](in: A): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    val out = new ObjectOutputStream(bos)
    out.writeObject(in)
    out.close()
    bos.toByteArray
  }

  def readObj[A](bytes: Array[Byte]): A = {
    val in  = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val obj = in.readObject()
    in.close()
    obj.asInstanceOf[A]
  }

  /**
   * Implicit serializers using Java I/O
   */
  implicit object SetupSerializer extends ParamSetSerializer[Setup] {
    def write(in: Setup): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): Setup = readObj[Setup](bytes)
  }

  implicit object ObserveSerializer extends ParamSetSerializer[Observe] {
    def write(in: Observe): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): Observe = readObj[Observe](bytes)
  }

  implicit object WaitSerializer extends ParamSetSerializer[Wait] {
    def write(in: Wait): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): Wait = readObj[Wait](bytes)
  }

  implicit object SequenceCommandSerializer extends ParamSetSerializer[SequenceCommand] {
    def write(in: SequenceCommand): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): SequenceCommand = readObj[SequenceCommand](bytes)
  }

  implicit object ControlCommandSerializer extends ParamSetSerializer[ControlCommand] {
    def write(in: ControlCommand): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): ControlCommand = readObj[ControlCommand](bytes)
  }

  implicit object StatusEventSerializer extends ParamSetSerializer[StatusEvent] {
    def write(in: StatusEvent): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): StatusEvent = readObj[StatusEvent](bytes)
  }

  implicit object ObserveEventSerializer extends ParamSetSerializer[ObserveEvent] {
    def write(in: ObserveEvent): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): ObserveEvent = readObj[ObserveEvent](bytes)
  }

  implicit object SystemEventSerializer extends ParamSetSerializer[SystemEvent] {
    def write(in: SystemEvent): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): SystemEvent = readObj[SystemEvent](bytes)
  }

  implicit object EventServiceEventSerializer extends ParamSetSerializer[EventServiceEvent] {
    def write(in: EventServiceEvent): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): EventServiceEvent = readObj[EventServiceEvent](bytes)
  }

  implicit object DemandStateSerializer extends ParamSetSerializer[DemandState] {
    def write(in: DemandState): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): DemandState = readObj[DemandState](bytes)
  }

  implicit object CurrentStateSerializer extends ParamSetSerializer[CurrentState] {
    def write(in: CurrentState): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): CurrentState = readObj[CurrentState](bytes)
  }

  implicit object StateVariableSerializer extends ParamSetSerializer[StateVariable] {
    def write(in: StateVariable): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): StateVariable = readObj[StateVariable](bytes)
  }

  implicit object CurrentStatesSerializer extends ParamSetSerializer[CurrentStates] {
    def write(in: CurrentStates): Array[Byte] = writeObj(in)

    def read(bytes: Array[Byte]): CurrentStates = readObj[CurrentStates](bytes)
  }

}
