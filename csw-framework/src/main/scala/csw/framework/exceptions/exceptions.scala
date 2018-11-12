package csw.framework.exceptions

/**
 * Represents an exception that will cause the termination of component when thrown from any
 * component code. The onShutdown handler will be invoked next to facilitate graceful shutdown.
 *
 * @param message represents the description or cause of exception
 */
abstract class FailureStop(message: String) extends RuntimeException(message)

/**
 * Represents an exception that will cause the component to restart. The component’s state will be cleared/reinitialized
 * and onInitialize handler will be invoked.
 *
 * @param message represents the description or cause of exception
 */
abstract class FailureRestart(message: String) extends RuntimeException(message)

/**
 * UnableToParseOptions is thrown while starting the container or host app if any of the options is not valid
 */
private[framework] case object UnableToParseOptions
    extends RuntimeException("Could not parse command line options. See --help to know more.")

/**
 * ClusterSeedsNotFound is thrown while starting the container or host app if clusterSeeds are not provided or set in
 * environment variable
 */
private[framework] case object ClusterSeedsNotFound
    extends RuntimeException(
      "clusterSeeds setting is not specified either as env variable or system property. " +
      "Please check online documentation for this set-up."
    )

/**
 * InitializationFailed is thrown by supervisor of a component if component initialization fails and it is started in
 * standalone mode. In standalone mode initialization failure will cause the jvm process to shutdown and will require a
 * manual action to fix the problem and start the jvm process again.
 */
private[framework] case object InitializationFailed extends RuntimeException("Component TLA failed to initialize")
