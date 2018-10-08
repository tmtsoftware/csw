package csw

/**
 * == Params ==
 *
 * This project is intended to hold reusable models and params used throughout the csw source code.
 *
 * This also provides out of the box support to cater to the diverse communication requirements.
 * Consumer of this library will be able to create Commands, Events, States to store ParameterSets.
 *
 * == Imp Packages ==
 *
 * === Commands and Events ===
 *
 * This packages contains classes, traits and models used to create *commands* and *events*.
 * These are all based on type-safe keys and items (a set of values with optional units).
 * Each key has a specific type and the key's values must be of that type.
 *
 * Two types of [[csw.params.commands.Command]] are supported:
 *   - [[csw.params.commands.SequenceCommand]]
 *     - This commands are targeted to Sequencer. Subtypes of this are: Setup, Observe and Wait.
 *
 *   - [[csw.params.commands.ControlCommand]]
 *     - This commands are targeted to Assemblies and HCD's. Subtypes of this are: Setup and Observe.
 *
 * Following are the concrete commands supported by csw:
 *  - [[csw.params.commands.Setup]]
 *  - [[csw.params.commands.Observe]]
 *  - [[csw.params.commands.Wait]]
 *
 * Two types of [[csw.params.events.Event]] are supported:
 *  - [[csw.params.events.SystemEvent]]
 *  - [[csw.params.events.ObserveEvent]]
 *
 * === core ===
 *
 * This package supports serialization and deserialization of commands, events and state variables in JSON format [[csw.params.core.formats.JsonSupport]].
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
 * Following [[csw.params.core.generics.KeyType]] are supported by csw:
 *
 * {{{
 *
 *       +--------------+-------------------------+---------------------------+
 *       |  Primitive   |      Scala KeyType      |       Java KeyType        |
 *       +--------------+-------------------------+---------------------------+
 *       | Boolean      | KeyType.BooleanKey      | JKeyTypes.BooleanKey      |
 *       | Character    | KeyType.CharKey         | JKeyTypes.JCharKey        |
 *       | Byte         | KeyType.ByteKey         | JKeyTypes.ByteKey         |
 *       | Short        | KeyType.ShortKey        | JKeyTypes.ShortKey        |
 *       | Long         | KeyType.LongKey         | JKeyTypes.LongKey         |
 *       | Int          | KeyType.IntKey          | JKeyTypes.IntKey          |
 *       | Float        | KeyType.FloatKey        | JKeyTypes.FloatKey        |
 *       | Double       | KeyType.DoubleKey       | JKeyTypes.DoubleKey       |
 *       | String       | KeyType.StringKey       | JKeyTypes.StringKey       |
 *       | Timestamp    | KeyType.TimestampKey    | JKeyTypes.TimestampKey    |
 *       | ----------   | ----------              | ----------                |
 *       | ByteArray    | KeyType.ByteArrayKey    | JKeyTypes.ByteArrayKey    |
 *       | ShortArray   | KeyType.ShortArrayKey   | JKeyTypes.ShortArrayKey   |
 *       | LongArray    | KeyType.LongArrayKey    | JKeyTypes.LongArrayKey    |
 *       | IntArray     | KeyType.IntArrayKey     | JKeyTypes.IntArrayKey     |
 *       | FloatArray   | KeyType.FloatArrayKey   | JKeyTypes.FloatArrayKey   |
 *       | DoubleArray  | KeyType.DoubleArrayKey  | JKeyTypes.DoubleArrayKey  |
 *       | ----------   | ----------              | ----------                |
 *       | ByteMatrix   | KeyType.ByteMatrixKey   | JKeyTypes.ByteMatrixKey   |
 *       | ShortMatrix  | KeyType.ShortMatrixKey  | JKeyTypes.ShortMatrixKey  |
 *       | LongMatrix   | KeyType.LongMatrixKey   | JKeyTypes.LongMatrixKey   |
 *       | IntMatrix    | KeyType.IntMatrixKey    | JKeyTypes.IntMatrixKey    |
 *       | FloatMatrix  | KeyType.FloatMatrixKey  | JKeyTypes.FloatMatrixKey  |
 *       | DoubleMatrix | KeyType.DoubleMatrixKey | JKeyTypes.DoubleMatrixKey |
 *       | ----------   | ----------              | ----------                |
 *       | Choice       | KeyType.ChoiceKey       | JKeyTypes.ChoiceKey       |
 *       | RaDec        | KeyType.RaDecKey        | JKeyTypes.RaDecKey        |
 *       | Struct       | KeyType.StructKey       | JKeyTypes.StructKey       |
 *       +--------------+-------------------------+---------------------------+
 *
 * }}}
 *
 * Detailed information about creating Keys and Parameters can be found here:
 *  https://tmtsoftware.github.io/csw/services/messages/keys-parameters.html
 *
 * Detailed information about creating commands can be found here:
 *  https://tmtsoftware.github.io/csw/services/messages/commands.html
 *
 * Detailed information about creating events can be found here:
 *  https://tmtsoftware.github.io/csw/services/messages/events.html
 */
package object params {}
