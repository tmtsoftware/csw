package csw.param

import java.lang
import java.time.Instant

import csw.param.Events._
import csw.param.Parameters._
import csw.param.StateVariable._
import csw.param.UnitsOfMeasure.Units
import csw.param.parameters._
import csw.param.parameters.arrays._
import csw.param.parameters.matrices._
import csw.param.parameters.primitives._
import spray.json._

/**
 * Supports conversion of commands and events to/from JSON
 */
//noinspection TypeAnnotation
object ParameterSetJson extends DefaultJsonProtocol with JavaFormatters {
  implicit val unitsFormat = jsonFormat1(Units.apply)

  // JSON formats
  implicit val charParameterFormat         = jsonFormat3(CharParameter.apply)
  implicit val shortParameterFormat        = jsonFormat3(ShortParameter.apply)
  implicit val intParameterFormat          = jsonFormat3(IntParameter.apply)
  implicit val longParameterFormat         = jsonFormat3(LongParameter.apply)
  implicit val floatParameterFormat        = jsonFormat3(FloatParameter.apply)
  implicit val doubleParameterFormat       = jsonFormat3(DoubleParameter.apply)
  implicit val stringParameterFormat       = jsonFormat3(StringParameter.apply)
  implicit val doubleMatrixParameterFormat = jsonFormat3(DoubleMatrixParameter.apply)
  implicit val doubleArrayParameterFormat  = jsonFormat3(DoubleArrayParameter.apply)
  implicit val floatMatrixParameterFormat  = jsonFormat3(FloatMatrixParameter.apply)
  implicit val floatArrayParameterFormat   = jsonFormat3(FloatArrayParameter.apply)
  implicit val intMatrixParameterFormat    = jsonFormat3(IntMatrixParameter.apply)
  implicit val intArrayParameterFormat     = jsonFormat3(IntArrayParameter.apply)
  implicit val byteMatrixParameterFormat   = jsonFormat3(ByteMatrixParameter.apply)
  implicit val byteArrayParameterFormat    = jsonFormat3(ByteArrayParameter.apply)
  implicit val shortMatrixParameterFormat  = jsonFormat3(ShortMatrixParameter.apply)
  implicit val shortArrayParameterFormat   = jsonFormat3(ShortArrayParameter.apply)
  implicit val longMatrixParameterFormat   = jsonFormat3(LongMatrixParameter.apply)
  implicit val longArrayParameterFormat    = jsonFormat3(LongArrayParameter.apply)
  implicit val choiceFormat                = jsonFormat1(Choice.apply)
  implicit val choicesFormat               = jsonFormat1(Choices.apply)
  implicit val choiceParameterFormat       = jsonFormat4(ChoiceParameter.apply)
  implicit val structParameterFormat       = jsonFormat3(StructParameter.apply)

  implicit def parameterFormat[T: JsonFormat]: RootJsonFormat[GParam[T]] = new RootJsonFormat[GParam[T]] {
    override def write(obj: GParam[T]): JsValue = {
      JsObject(
        "typeName" -> JsString(obj.typeName),
        "keyName"  -> JsString(obj.keyName),
        "values"   -> JsArray(obj.values.map(implicitly[JsonFormat[T]].write)),
        "units"    -> unitsFormat.write(obj.units)
      )
    }

    override def read(json: JsValue): GParam[T] = {
      val fields = json.asJsObject.fields
      GParam(
        fields("typeName").convertTo[String],
        fields("keyName").convertTo[String],
        fields("values").convertTo[Vector[T]],
        fields("units").convertTo[Units],
        implicitly[JsonFormat[T]]
      )
    }
  }

  implicit def structFormat: JsonFormat[Struct] = new JsonFormat[Struct] {
    def write(s: Struct): JsValue = JsObject(
      "type"     -> JsString(s.typeName),
      "paramSet" -> s.paramSet.toJson
    )

    def read(json: JsValue): Struct = {
      json match {
        case JsObject(fields) =>
          (fields("type"), fields("name"), fields("paramSet")) match {
            case (JsString(typeName), JsString(name), paramSet) =>
              typeName match {
                case `structType` => Struct(paramSetFormat.read(paramSet))
                case _            => unexpectedJsValueError(json)
              }
            case _ => unexpectedJsValueError(json)
          }
        case _ => unexpectedJsValueError(json)
      }
    }
  }

  implicit def subsystemFormat: JsonFormat[Subsystem] = new JsonFormat[Subsystem] {
    def write(obj: Subsystem) = JsString(obj.name)

    def read(value: JsValue): Subsystem = {
      value match {
        case JsString(subsystemStr) =>
          Subsystem.lookup(subsystemStr) match {
            case Some(subsystem) => subsystem
            case None            => Subsystem.BAD
          }
        // With malformed JSON, return BAD
        case _ => Subsystem.BAD
      }
    }
  }

  implicit def paramSetFormat: JsonFormat[ParameterSet] = new JsonFormat[ParameterSet] {
    def write(parameters: ParameterSet) = JsArray(parameters.map(writeParameter(_)).toList: _*)

    def read(json: JsValue) = json match {
      case a: JsArray => a.elements.map((el: JsValue) => readParameterAndType(el)).toSet
      case _          => unexpectedJsValueError(json)
    }
  }

  implicit def eventTimeFormat: JsonFormat[EventTime] = new JsonFormat[EventTime] {
    def write(et: EventTime): JsValue = JsString(et.toString)

    def read(json: JsValue): EventTime = json match {
      case JsString(s) => Instant.parse(s)
      case _           => unexpectedJsValueError(json)
    }
  }

  implicit val parameterSetKeyFormat = jsonFormat2(Prefix.apply)
  implicit val obsIdFormat           = jsonFormat1(ObsId.apply)
  implicit val eventInfoFormat       = jsonFormat4(EventInfo.apply)

  // JSON type tags
  private val charType            = classOf[CharParameter].getSimpleName
  private val shortType           = classOf[ShortParameter].getSimpleName
  private val integerType         = classOf[IntParameter].getSimpleName
  private val longType            = classOf[LongParameter].getSimpleName
  private val floatType           = classOf[FloatParameter].getSimpleName
  private val doubleType          = classOf[DoubleParameter].getSimpleName
  private val stringType          = classOf[StringParameter].getSimpleName
  private val doubleMatrixType    = classOf[DoubleMatrixParameter].getSimpleName
  private val doubleArrayType     = classOf[DoubleArrayParameter].getSimpleName
  private val floatMatrixType     = classOf[FloatMatrixParameter].getSimpleName
  private val floatArrayType      = classOf[FloatArrayParameter].getSimpleName
  private val intMatrixType       = classOf[IntMatrixParameter].getSimpleName
  private val intArrayType        = classOf[IntArrayParameter].getSimpleName
  private val byteMatrixType      = classOf[ByteMatrixParameter].getSimpleName
  private val byteArrayType       = classOf[ByteArrayParameter].getSimpleName
  private val shortMatrixType     = classOf[ShortMatrixParameter].getSimpleName
  private val shortArrayType      = classOf[ShortArrayParameter].getSimpleName
  private val longMatrixType      = classOf[LongMatrixParameter].getSimpleName
  private val longArrayType       = classOf[LongArrayParameter].getSimpleName
  private val choiceType          = classOf[ChoiceParameter].getSimpleName
  private val structParameterType = classOf[StructParameter].getSimpleName

  // config and event type JSON tags
  private val setupType        = classOf[Setup].getSimpleName
  private val observeType      = classOf[Observe].getSimpleName
  private val waitType         = classOf[Wait].getSimpleName
  private val statusEventType  = classOf[StatusEvent].getSimpleName
  private val observeEventType = classOf[ObserveEvent].getSimpleName
  private val systemEventType  = classOf[SystemEvent].getSimpleName
  private val curentStateType  = classOf[CurrentState].getSimpleName
  private val demandStateType  = classOf[DemandState].getSimpleName
  private val structType       = classOf[Struct].getSimpleName

  private def unexpectedJsValueError(x: JsValue) = deserializationError(s"Unexpected JsValue: $x")

  // XXX TODO Use JNumber?
  def writeParameter[S, I /*, J */ ](parameter: Parameter[S /*, J */ ]): JsValue = {
    val result: (JsString, JsValue) = parameter match {
      case i: CharParameter         => (JsString(charType), charParameterFormat.write(i))
      case i: ShortParameter        => (JsString(shortType), shortParameterFormat.write(i))
      case i: IntParameter          => (JsString(integerType), intParameterFormat.write(i))
      case i: LongParameter         => (JsString(longType), longParameterFormat.write(i))
      case i: FloatParameter        => (JsString(floatType), floatParameterFormat.write(i))
      case i: DoubleParameter       => (JsString(doubleType), doubleParameterFormat.write(i))
      case i: StringParameter       => (JsString(stringType), stringParameterFormat.write(i))
      case i: DoubleMatrixParameter => (JsString(doubleMatrixType), doubleMatrixParameterFormat.write(i))
      case i: DoubleArrayParameter  => (JsString(doubleArrayType), doubleArrayParameterFormat.write(i))
      case i: FloatMatrixParameter  => (JsString(floatMatrixType), floatMatrixParameterFormat.write(i))
      case i: FloatArrayParameter   => (JsString(floatArrayType), floatArrayParameterFormat.write(i))
      case i: IntMatrixParameter    => (JsString(intMatrixType), intMatrixParameterFormat.write(i))
      case i: IntArrayParameter     => (JsString(intArrayType), intArrayParameterFormat.write(i))
      case i: ByteMatrixParameter   => (JsString(byteMatrixType), byteMatrixParameterFormat.write(i))
      case i: ByteArrayParameter    => (JsString(byteArrayType), byteArrayParameterFormat.write(i))
      case i: ShortMatrixParameter  => (JsString(shortMatrixType), shortMatrixParameterFormat.write(i))
      case i: ShortArrayParameter   => (JsString(shortArrayType), shortArrayParameterFormat.write(i))
      case i: LongMatrixParameter   => (JsString(longMatrixType), longMatrixParameterFormat.write(i))
      case i: LongArrayParameter    => (JsString(longArrayType), longArrayParameterFormat.write(i))
      case i: ChoiceParameter       => (JsString(choiceType), choiceParameterFormat.write(i))
      case i: StructParameter       => (JsString(structParameterType), structParameterFormat.write(i))
      case i: GParam[_]             => (JsString(i.typeName), i.toJson)
    }
    JsObject("type" -> result._1, "parameter" -> result._2)
  }

  def readParameterAndType(json: JsValue): Parameter[_ /*, _ */ ] = json match {
    case JsObject(fields) =>
      (fields("type"), fields("parameter")) match {
        case (JsString(`charType`), parameter)            => charParameterFormat.read(parameter)
        case (JsString(`shortType`), parameter)           => shortParameterFormat.read(parameter)
        case (JsString(`integerType`), parameter)         => intParameterFormat.read(parameter)
        case (JsString(`longType`), parameter)            => longParameterFormat.read(parameter)
        case (JsString(`floatType`), parameter)           => floatParameterFormat.read(parameter)
        case (JsString(`doubleType`), parameter)          => doubleParameterFormat.read(parameter)
        case (JsString(`stringType`), parameter)          => stringParameterFormat.read(parameter)
        case (JsString(`doubleMatrixType`), parameter)    => doubleMatrixParameterFormat.read(parameter)
        case (JsString(`doubleArrayType`), parameter)     => doubleArrayParameterFormat.read(parameter)
        case (JsString(`floatMatrixType`), parameter)     => floatMatrixParameterFormat.read(parameter)
        case (JsString(`floatArrayType`), parameter)      => floatArrayParameterFormat.read(parameter)
        case (JsString(`intMatrixType`), parameter)       => intMatrixParameterFormat.read(parameter)
        case (JsString(`intArrayType`), parameter)        => intArrayParameterFormat.read(parameter)
        case (JsString(`byteMatrixType`), parameter)      => byteMatrixParameterFormat.read(parameter)
        case (JsString(`byteArrayType`), parameter)       => byteArrayParameterFormat.read(parameter)
        case (JsString(`shortMatrixType`), parameter)     => shortMatrixParameterFormat.read(parameter)
        case (JsString(`shortArrayType`), parameter)      => shortArrayParameterFormat.read(parameter)
        case (JsString(`longMatrixType`), parameter)      => longMatrixParameterFormat.read(parameter)
        case (JsString(`longArrayType`), parameter)       => longArrayParameterFormat.read(parameter)
        case (JsString(`choiceType`), parameter)          => choiceParameterFormat.read(parameter)
        case (JsString(`structParameterType`), parameter) => structParameterFormat.read(parameter)
        case (JsString(name), parameter) =>
          Formats.get(name) match {
            case None         ⇒ unexpectedJsValueError(parameter)
            case Some(format) ⇒ format.read(parameter)
          }
        case _ => unexpectedJsValueError(json)
      }
    case _ => unexpectedJsValueError(json)
  }

  /**
   * Handles conversion of ParameterSetInfo to/from JSON
   */
  implicit def CommandInfoFormat: RootJsonFormat[CommandInfo] = new RootJsonFormat[CommandInfo] {
    override def read(json: JsValue): CommandInfo = json.asJsObject.getFields("obsId", "runId") match {
      case Seq(JsString(obsId), JsString(runId)) =>
        CommandInfo(ObsId(obsId), RunId(runId))
    }

    override def write(obj: CommandInfo): JsValue = JsObject(
      "obsId" -> JsString(obj.obsId.obsId),
      "runId" -> JsString(obj.runId.id)
    )
  }

  /**
   * Writes a SequenceParameterSet to JSON
   *
   * @param sequenceCommand any instance of SequenceCommand
   * @tparam A the type of the command (implied)
   * @return a JsValue object representing the SequenceCommand
   */
  def writeSequenceCommand[A <: SequenceCommand](sequenceCommand: A): JsValue = {
    JsObject(
      "type"     -> JsString(sequenceCommand.typeName),
      "info"     -> CommandInfoFormat.write(sequenceCommand.info),
      "prefix"   -> parameterSetKeyFormat.write(sequenceCommand.prefix),
      "paramSet" -> sequenceCommand.paramSet.toJson
    )
  }

  /**
   * Reads a SequenceCommand back from JSON
   *
   * @param json the parsed JSON
   * @return an instance of the given SequenceCommand type, or an exception if the JSON is not valid for that type
   */
  def readSequenceCommand[A <: SequenceCommand](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("info"), fields("prefix"), fields("paramSet")) match {
          case (JsString(typeName), info, prefix, paramSet) =>
            val commandInfo = info.convertTo[CommandInfo]
            val ck          = prefix.convertTo[Prefix]
            typeName match {
              case `setupType`   => Setup(commandInfo, ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `observeType` => Observe(commandInfo, ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `waitType`    => Wait(commandInfo, ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case _             => unexpectedJsValueError(json)
            }
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }

  /**
   * Writes a state variable to JSON
   *
   * @param stateVariable any instance of StateVariable
   * @tparam A the type of the StateVariable (implied)
   * @return a JsValue object representing the StateVariable
   */
  def writeStateVariable[A <: StateVariable](stateVariable: A): JsValue = {
    JsObject(
      "type"     -> JsString(stateVariable.typeName),
      "prefix"   -> parameterSetKeyFormat.write(stateVariable.prefix),
      "paramSet" -> stateVariable.paramSet.toJson
    )
  }

  /**
   * Reads a StateVariable back from JSON
   *
   * @param json the parsed JSON
   * @return an instance of the given StateVariable, or an exception if the JSON is not valid for that type
   */
  def readStateVariable[A <: StateVariable](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("prefix"), fields("paramSet")) match {
          case (JsString(typeName), prefix, paramSet) =>
            val ck = prefix.convertTo[Prefix]
            typeName match {
              case `curentStateType` => CurrentState(ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `demandStateType` => DemandState(ck, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case _                 => unexpectedJsValueError(json)
            }
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }

  /**
   * Writes an event to JSON
   *
   * @param event any instance of EventType
   * @tparam A the type of the event (implied)
   * @return a JsValue object representing the event
   */
  def writeEvent[A <: EventType[_]](event: A): JsValue = {
    JsObject(
      "type"     -> JsString(event.typeName),
      "info"     -> eventInfoFormat.write(event.info),
      "paramSet" -> event.paramSet.toJson
    )
  }

  /**
   * Reads an event back from JSON
   *
   * @param json the parsed JSON
   * @tparam A the type of the event (use Any and match on the type if you don't know)
   * @return an instance of the given event type, or an exception if the JSON is not valid for that type
   */
  def readEvent[A <: EventType[_]](json: JsValue): A = {
    json match {
      case JsObject(fields) =>
        (fields("type"), fields("info"), fields("paramSet")) match {
          case (JsString(typeName), eventInfo, paramSet) =>
            val info = eventInfo.convertTo[EventInfo]
            typeName match {
              case `statusEventType`  => StatusEvent(info, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `observeEventType` => ObserveEvent(info, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case `systemEventType`  => SystemEvent(info, paramSetFormat.read(paramSet)).asInstanceOf[A]
              case _                  => unexpectedJsValueError(json)
            }
          case _ => unexpectedJsValueError(json)
        }
      case _ => unexpectedJsValueError(json)
    }
  }
}

trait JavaFormatters { self: DefaultJsonProtocol =>
  //JSON Formats for Java by converting scala types to java types.
  implicit val jIntergerFormat: JsonFormat[Integer]     = IntJsonFormat.asInstanceOf[JsonFormat[java.lang.Integer]]
  implicit val jBooleanFormat: JsonFormat[lang.Boolean] = BooleanJsonFormat.asInstanceOf[JsonFormat[java.lang.Boolean]]
}
