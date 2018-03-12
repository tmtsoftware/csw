package csw

/**
 * == Messages ==
 *
 * This project is intended to hold reusable models and messages used throughout the csw source code.
 *
 * This also provides out of the box support to cater to the diverse communication requirements.
 * Consumer of this library will be able to create Commands, Events, States to store ParameterSets.
 *
 * == Imp Packages ==
 *
 * === commands and events ===
 *
 * This packages contains classes, traits and models used to create *commands* and *events*.
 * These are all based on type-safe keys and items (a set of values with optional units).
 * Each key has a specific type and the key's values must be of that type.
 *
 * Two types of [[csw.messages.commands.Command]] are supported:
 *   - [[csw.messages.commands.SequenceCommand]]
 *     - This commands are targeted to Sequencer. Subtypes of this are: Setup, Observe and Wait.
 *
 *   - [[csw.messages.commands.ControlCommand]]
 *     - This commands are targeted to Assemblies and HCD's. Subtypes of this are: Setup and Observe.
 *
 * Following are the concrete commands supported by csw:
 *  - [[csw.messages.commands.Setup]]
 *  - [[csw.messages.commands.Observe]]
 *  - [[csw.messages.commands.Wait]]
 *
 * Two types of [[csw.messages.events.Event]] are supported:
 *  - [[csw.messages.events.SystemEvent]]
 *  - [[csw.messages.events.ObserveEvent]]
 *
 * Another important feature provided by *commands* package is [[csw.messages.commands.matchers.Matcher]]
 * One of the use case for using matcher is when Assembly sends [[csw.messages.scaladsl.CommandMessage.Oneway]] command to HCD
 * and in response to this command HCD keeps publishing its current state.
 * Then Assembly can use Matcher with the matching definition as provided by [[csw.messages.commands.matchers.StateMatcher]] to
 * match against the current states published by HCD.
 *
 * === location and framework ===
 *
 * This packages contains reusable classes, traits and models.
 *
 * === params ===
 *
 * This package supports serialization and deserialization of commands, events and state variables in following formats:
 *  - JSON : [[csw.messages.params.formats.JsonSupport]]
 *  - Kryo : [[csw.messages.params.generics.ParamSetSerializer]]
 *  - Protobuf : package pb contains utility and directory protobuf contains proto schema files.
 *
 * === Scala and Java APIs ===
 *
 * All the param and event classes are immutable. The `set` methods return a new instance of the object with a
 * new item added and the `get` methods return an Option, in case the Key is not found. There are also `value` methods
 * that return a value directly, throwing an exception if the key or value is not found.
 *
 * === Key Types ===
 *
 * A set of standard key types and matching items are defined. Each key accepts one or more values
 * of the given type.
 *
 * Following [[csw.messages.params.generics.KeyType]] are supported by csw:
 *
 *         +--------------+-------------------------+---------------------------+
 *         |  Primitive   |      Scala KeyType      |       Java KeyType        |
 *         +--------------+-------------------------+---------------------------+
 *         | Boolean      | KeyType.BooleanKey      | JKeyTypes.BooleanKey      |
 *         | Character    | KeyType.CharKey         | JKeyTypes.JCharKey        |
 *         | Byte         | KeyType.ByteKey         | JKeyTypes.ByteKey         |
 *         | Short        | KeyType.ShortKey        | JKeyTypes.ShortKey        |
 *         | Long         | KeyType.LongKey         | JKeyTypes.LongKey         |
 *         | Int          | KeyType.IntKey          | JKeyTypes.IntKey          |
 *         | Float        | KeyType.FloatKey        | JKeyTypes.FloatKey        |
 *         | Double       | KeyType.DoubleKey       | JKeyTypes.DoubleKey       |
 *         | String       | KeyType.StringKey       | JKeyTypes.StringKey       |
 *         | Timestamp    | KeyType.TimestampKey    | JKeyTypes.TimestampKey    |
 *         | ----------   | ----------              | ----------                |
 *         | ByteArray    | KeyType.ByteArrayKey    | JKeyTypes.ByteArrayKey    |
 *         | ShortArray   | KeyType.ShortArrayKey   | JKeyTypes.ShortArrayKey   |
 *         | LongArray    | KeyType.LongArrayKey    | JKeyTypes.LongArrayKey    |
 *         | IntArray     | KeyType.IntArrayKey     | JKeyTypes.IntArrayKey     |
 *         | FloatArray   | KeyType.FloatArrayKey   | JKeyTypes.FloatArrayKey   |
 *         | DoubleArray  | KeyType.DoubleArrayKey  | JKeyTypes.DoubleArrayKey  |
 *         | ----------   | ----------              | ----------                |
 *         | ByteMatrix   | KeyType.ByteMatrixKey   | JKeyTypes.ByteMatrixKey   |
 *         | ShortMatrix  | KeyType.ShortMatrixKey  | JKeyTypes.ShortMatrixKey  |
 *         | LongMatrix   | KeyType.LongMatrixKey   | JKeyTypes.LongMatrixKey   |
 *         | IntMatrix    | KeyType.IntMatrixKey    | JKeyTypes.IntMatrixKey    |
 *         | FloatMatrix  | KeyType.FloatMatrixKey  | JKeyTypes.FloatMatrixKey  |
 *         | DoubleMatrix | KeyType.DoubleMatrixKey | JKeyTypes.DoubleMatrixKey |
 *         | ----------   | ----------              | ----------                |
 *         | Choice       | KeyType.ChoiceKey       | JKeyTypes.ChoiceKey       |
 *         | RaDec        | KeyType.RaDecKey        | JKeyTypes.RaDecKey        |
 *         | Struct       | KeyType.StructKey       | JKeyTypes.StructKey       |
 *         +--------------+-------------------------+---------------------------+
 *
 * Detailed information about creating Keys and Parameters can be found here:
 *  https://tmtsoftware.github.io/csw-prod/services/messages/keys-parameters.html
 *
 * Detailed information about creating commands can be found here:
 *  https://tmtsoftware.github.io/csw-prod/services/messages/commands.html
 *
 * Detailed information about creating events can be found here:
 *  https://tmtsoftware.github.io/csw-prod/services/messages/events.html
 */
package object messages {}
