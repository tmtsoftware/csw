package csw.param

import csw.param.parameters.{Key, Parameter}

import scala.annotation.varargs
import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
 * Support for sets of generic, type-safe command or event parameters
 * (key/value objects with units)
 */
object Parameters {

  /**
   * Combines subsystem and the subsystem's prefix
   *
   * @param subsystem the subsystem that is the target of the command
   * @param prefix    the subsystem's prefix
   */
  case class Prefix(subsystem: Subsystem, prefix: String) {
    override def toString = s"[$subsystem, $prefix]"

    /**
     * Creates a Prefix from the given string
     *
     * @return a Prefix object parsed for the subsystem and prefix
     */
    def this(prefix: String) {
      this(Prefix.subsystem(prefix), prefix)
    }
  }

  /**
   * A top level key for a parameter set: combines subsystem and the subsystem's prefix
   */
  object Prefix {
    private val SEPARATOR = '.'

    /**
     * Creates a Prefix from the given string
     *
     * @return an Prefix object parsed for the subsystem and prefix
     */
    implicit def stringToPrefix(prefix: String): Prefix = Prefix(subsystem(prefix), prefix)

    private def subsystem(keyText: String): Subsystem = {
      assert(keyText != null)
      Subsystem.lookup(keyText.splitAt(keyText.indexOf(SEPARATOR))._1).getOrElse(Subsystem.BAD)
    }
  }

  type ParameterSet = Set[Parameter[_]]

  /**
   * A trait to be mixed in that provides a parameter set and prefix info
   */
  trait ParameterSetKeyData { self: ParameterSetType[_] =>

    /**
     * Returns an object providing the subsystem and prefix for the parameter set
     */
    def prefix: Prefix

    /**
     * The subsystem for the parameter set
     */
    final def subsystem: Subsystem = prefix.subsystem

    /**
     * The prefix for the parameter set
     */
    final def prefixStr: String = prefix.prefix

    // This is the get for a Setup/Observe/Wait
    override def toString = s"$typeName([$subsystem, $prefixStr]$dataToString)"
  }

  /**
   * The base trait for various parameter set types (commands or events)
   *
   * @tparam T the subclass of ParameterSetType
   */
  trait ParameterSetType[T <: ParameterSetType[T]] { self: T =>

    /**
     * A name identifying the type of parameter set, such as "setup", "observe".
     * This is used in the JSON and toString output.
     */
    def typeName: String = getClass.getSimpleName

    /**
     * Holds the parameters for this parameter set
     */
    def paramSet: ParameterSet

    /**
     * The number of parameters in this parameter set
     *
     * @return the number of parameters in the parameter set
     */
    def size: Int = paramSet.size

    /**
     * Adds a parameter to the parameter set
     *
     * @param parameter the parameter to add
     * @tparam P the Parameter type
     * @return a new instance of this parameter set with the given parameter added
     */
    def add[P <: Parameter[_]](parameter: P): T = doAdd(this, parameter)

    private def doAdd[P <: Parameter[_]](c: T, parameter: P): T = {
      val paramSetRemoved: T = removeByKeyname(c, parameter.keyName)
      create(paramSetRemoved.paramSet + parameter)
    }

    /**
     * Adds several parameters to the parameter set
     *
     * @param parametersToAdd the list of parameters to add to the parameter set
     * @tparam P must be a subclass of Parameter
     * @return a new instance of this parameter set with the given parameter added
     */
    def madd[P <: Parameter[_]](parametersToAdd: P*): T =
      parametersToAdd.foldLeft(this)((c, parameter) => doAdd(c, parameter))

    /**
     * Returns an Option with the parameter for the key if found, otherwise None
     *
     * @param key the Key to be used for lookup
     * @return the parameter for the key, if found
     * @tparam S the Scala value type
     * @tparam P the parameter type for the Scala value S
     */
    def get[S, P <: Parameter[S]](key: Key[S, P]): Option[P] = getByKeyname[P](paramSet, key.keyName)

    /**
     * Returns an Option with the parameter for the key if found, otherwise None. Access with keyname rather
     * than Key
     *
     * @param keyName the keyname to be used for the lookup
     * @tparam P the value type
     */
    def getByName[P <: Parameter[_]](keyName: String): Option[P] = getByKeyname[P](paramSet, keyName)

    def find[P <: Parameter[_]](parameter: P): Option[P] = getByKeyname[P](paramSet, parameter.keyName)

    /**
     * Return the parameter associated with a Key rather than an Option
     *
     * @param key the Key to be used for lookup
     * @tparam S the Scala value type
     * @tparam P the Parameter type associated with S
     * @return the parameter associated with the Key or a NoSuchElementException if the key does not exist
     */
    final def apply[S, P <: Parameter[S]](key: Key[S, P]): P = get(key).get

    /**
     * Returns the actual parameter associated with a key
     *
     * @param key the Key to be used for lookup
     * @tparam S the Scala value type
     * @tparam P the Parameter type associated with S
     * @return the parameter associated with the key or a NoSuchElementException if the key does not exist
     */
    final def parameter[S, P <: Parameter[S]](key: Key[S, P]): P = get(key).get

    /**
     * Returns true if the key exists in the parameter set
     *
     * @param key the key to check for
     * @return true if the key is found
     * @tparam S the Scala value type
     * @tparam P the type of the Parameter associated with the key
     */
    def exists[S, P <: Parameter[S]](key: Key[S, P]): Boolean = get(key).isDefined

    /**
     * Remove a parameter from the parameter set by key
     *
     * @param key the Key to be used for removal
     * @tparam S the Scala value type
     * @tparam P the parameter type used with Scala type S
     * @return a new T, where T is a parameter set child with the key removed or identical if the key is not present
     */
    def remove[S, P <: Parameter[S]](key: Key[S, P]): T = removeByKeyname(this, key.keyName) //doRemove(this, key)

    /**
     * Removes a parameter based on the parameter
     *
     * @param parameter to be removed from the parameter set
     * @tparam P the type of the parameter to be removed
     * @return a new T, where T is a parameter set child with the parameter removed or identical if the parameter is not present
     */
    def remove[P <: Parameter[_]](parameter: P): T = removeByParameter(this, parameter)

    /**
     * Function removes a parameter from the parameter set c based on keyname
     *
     * @param c       the parameter set to remove from
     * @param keyname the key name of the parameter to remove
     * @tparam P the Parameter type
     * @return a new T, where T is a parameter set child with the parameter removed or identical if the parameter is not present
     */
    private def removeByKeyname[P <: Parameter[_]](c: ParameterSetType[T], keyname: String): T = {
      val f: Option[P] = getByKeyname(c.paramSet, keyname)
      f match {
        case Some(parameter) => create(c.paramSet.-(parameter))
        case None            => c.asInstanceOf[T] //create(c.parameters) also works
      }
    }

    /**
     * Function removes a parameter from the parameter set c based on parameter content
     *
     * @param c           the parameter set to remove from
     * @param parameterIn the parameter that should be removed
     * @tparam P the Parameter type
     * @return a new T, where T is a parameter set child with the parameter removed or identical if the parameter is not presen
     */
    private def removeByParameter[P <: Parameter[_]](c: ParameterSetType[T], parameterIn: P): T = {
      val f: Option[P] = getByParameter(c.paramSet, parameterIn)
      f match {
        case Some(parameter) => create(c.paramSet.-(parameter))
        case None            => c.asInstanceOf[T]
      }
    }

    // Function to find a parameter by keyname - made public to enable matchers
    private def getByKeyname[P](parametersIn: ParameterSet, keyname: String): Option[P] =
      parametersIn.find(_.keyName == keyname).asInstanceOf[Option[P]]

    // Function to find a given parameter in the parameter set
    private def getByParameter[P](parametersIn: ParameterSet, parameter: Parameter[_]): Option[P] =
      parametersIn.find(_.equals(parameter)).asInstanceOf[Option[P]]

    /**
     * Method called by subclass to create a copy with the same key (or other fields) and new parameters
     */
    protected def create(data: ParameterSet): T

    protected def dataToString: String = paramSet.mkString("(", ", ", ")")

    override def toString = s"$typeName$dataToString"

    /**
     * Returns true if the data contains the given key
     */
    def contains(key: Key[_, _]): Boolean = paramSet.exists(_.keyName == key.keyName)

    /**
     * Returns a set containing the names of any of the given keys that are missing in the data
     *
     * @param keys one or more keys
     */
    def missingKeys(keys: Key[_, _]*): Set[String] = {
      val argKeySet        = keys.map(_.keyName).toSet
      val parametersKeySet = paramSet.map(_.keyName)
      argKeySet.diff(parametersKeySet)
    }

    /**
     * java API: Returns a set containing the names of any of the given keys that are missing in the data
     *
     * @param keys one or more keys
     */
    @varargs
    def jMissingKeys(keys: Key[_, _]*): java.util.Set[String] = missingKeys(keys: _*).asJava

    /**
     * Returns a map based on this object where the keys and values are in string get
     * (Could be useful for exporting in a get that other languages can read).
     * Derived classes might want to add values to this map for fixed fields.
     */
    def getStringMap: Map[String, String] =
      paramSet.map(i => i.keyName -> i.values.map(_.toString).mkString(",")).toMap
  }

  /**
   * This will include information related to the observation that is tied to a parameter set
   * This will grow and develop.
   *
   * @param obsId the observation id
   * @param runId unique ID for this parameter set
   */
  case class CommandInfo(obsId: ObsId, runId: RunId = RunId()) {

    /**
     * Creates an instance with the given obsId and a unique runId
     */
    def this(obsId: String) = this(ObsId(obsId))
  }

  object CommandInfo {
    implicit def strToParamSetInfo(obsId: String): CommandInfo = CommandInfo(ObsId(obsId))
  }

  /**
   * Common trait for Setup, Observe and Wait commands
   */
  sealed trait Command {

    /**
     * A name identifying the type of parameter set, such as "setup", "observe".
     * This is used in the JSON and toString output.
     */
    def typeName: String

    /**
     * information related to the parameter set
     */
    val info: CommandInfo

    /**
     * identifies the target subsystem
     */
    val prefix: Prefix

    /**
     * an optional initial set of parameters (keys with values)
     */
    val paramSet: ParameterSet
  }

  /**
   * Trait for sequence parameter sets
   */
  sealed trait SequenceCommand extends Command

  /**
   * Marker trait for control parameter sets
   */
  sealed trait ControlCommand extends Command

  /**
   * a parameter set for setting telescope and instrument parameters
   *
   * @param info     information related to the parameter set
   * @param prefix   identifies the target subsystem
   * @param paramSet an optional initial set of parameters (keys with values)
   */
  case class Setup(info: CommandInfo, prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[Setup]
      with ParameterSetKeyData
      with SequenceCommand
      with ControlCommand {

    override def create(data: ParameterSet) = Setup(info, prefix, data)

    // This is here for Java to construct with String
    def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[P <: Parameter[_]](parameter: P): Setup = super.add(parameter)

    override def remove[S, P <: Parameter[S]](key: Key[S, P]): Setup = super.remove(key)
  }

  /**
   * a parameter set for setting observation parameters
   *
   * @param info     information related to the parameter set
   * @param prefix   identifies the target subsystem
   * @param paramSet an optional initial set of parameters (keys with values)
   */
  case class Observe(info: CommandInfo, prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[Observe]
      with ParameterSetKeyData
      with SequenceCommand
      with ControlCommand {

    override def create(data: ParameterSet) = Observe(info, prefix, data)

    // This is here for Java to construct with String
    def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[P <: Parameter[_]](parameter: P): Observe = super.add(parameter)

    override def remove[S, P <: Parameter[S]](key: Key[S, P]): Observe = super.remove(key)
  }

  /**
   * a parameter set indicating a pause in processing
   *
   * @param info     information related to the parameter set
   * @param prefix   identifies the target subsystem
   * @param paramSet an optional initial set of parameters (keys with values)
   */
  case class Wait(info: CommandInfo, prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[Wait]
      with ParameterSetKeyData
      with SequenceCommand {

    override def create(data: ParameterSet) = Wait(info, prefix, data)

    // This is here for Java to construct with String
    def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[P <: Parameter[_]](parameter: P): Wait = super.add(parameter)

    override def remove[S, P <: Parameter[S]](key: Key[S, P]): Wait = super.remove(key)
  }

  /**
   * A parameters set for returning results
   *
   * @param info     information related to the parameter set
   * @param prefix   identifies the target subsystem
   * @param paramSet an optional initial set of parameters (keys with values)
   */
  case class Result(info: CommandInfo, prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[Result]
      with ParameterSetKeyData {

    override def create(data: ParameterSet) = Result(info, prefix, data)

    // This is here for Java to construct with String
    def this(info: CommandInfo, prefix: String) = this(info, Prefix.stringToPrefix(prefix))

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[P <: Parameter[_]](parameter: P): Result = super.add(parameter)

    override def remove[S, P <: Parameter[S]](key: Key[S, P]): Result = super.remove(key)
  }

  /**
   * Filters
   */
  object ParameterSetFilters {
    // A filter type for various parameter set data
    type ParamSetFilter[A] = A => Boolean

    def prefixes(paramSets: Seq[ParameterSetKeyData]): Set[String] = paramSets.map(_.prefixStr).toSet

    def onlySetups(paramSets: Seq[SequenceCommand]): Seq[Setup] = paramSets.collect { case ct: Setup => ct }

    def onlyObserves(paramSets: Seq[SequenceCommand]): Seq[Observe] = paramSets.collect { case ct: Observe => ct }

    def onlyWaits(paramSets: Seq[SequenceCommand]): Seq[Wait] = paramSets.collect { case ct: Wait => ct }

    val prefixStartsWithFilter: String => ParamSetFilter[ParameterSetKeyData] = query =>
      sc => sc.prefixStr.startsWith(query)
    val prefixContainsFilter: String => ParamSetFilter[ParameterSetKeyData] = query =>
      sc => sc.prefixStr.contains(query)
    val prefixIsSubsystem: Subsystem => ParamSetFilter[ParameterSetKeyData] = query => sc => sc.subsystem.equals(query)

    def prefixStartsWith(query: String, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
      paramSets.filter(prefixStartsWithFilter(query))

    def prefixContains(query: String, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
      paramSets.filter(prefixContainsFilter(query))

    def prefixIsSubsystem(query: Subsystem, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
      paramSets.filter(prefixIsSubsystem(query))
  }

  /**
   * Contains a list of commands that can be sent to a sequencer
   */
  final case class CommandList(paramSets: Seq[SequenceCommand])

}
