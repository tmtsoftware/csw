package csw.services.logging.commons

//TODO: explain better significance for singleton state
private[logging] object LoggingKeys {

  // SOURCE

  // Set in LogActor. Set to common unless alternative message type passed,
  // in which case the value is provided as part of message.
  // Each category logs to a different file, with category name
  // name embedded in filename.
  // This Key is not written to the log.
  val CATEGORY = "@category"

  // Full header keys, same for all messages by a System
  val HOST    = "@host"    // LoggingSystem - full header
  val NAME    = "@name"    // Logging System - full header (name of LoggingSystem)
  val VERSION = "@version" // LoggingSystem - full header (user supplied when instantiating LoggingSystem)
  val SERVICE = "@service" // this appears to be intended for the CSW service name and version. Not used except for custom appender in test. Should this be in full header?

  // The following keys are always present
  val TIMESTAMP = "timestamp" // Logger gets this when constructing Log Message
  val SEVERITY  = "@severity" // Set by logger, put into Log Message

  val FILE  = "file"  // source location macro
  val CLASS = "class" // source location marco (constructed as package.class from source location)
  val LINE  = "line"  // source location macro

  //  These items are only present if applicable.
  val COMPONENT_NAME = "@componentName" // Passed into LoggerImpl on instantiation of a ComponentLogger.  Passed in Log Message
  val ACTOR          = "actor"          // Passed into LoggerImpl on instantiation of a ComponentLogger.Actor.  Passed in Log Message

  // Following items are passed in by user, and are optional (except message)
  val MESSAGE  = "message"  // User, via logger methods.  Passed to actor in Log Message
  val MSG      = "@msg"     // User, via logger methods.  Passed to actor in Log message (for optional Map of user keys)
  val TRACE_ID = "@traceId" // User, via logger methods.  Passed to actor in Log Message
  val EX       = "ex"       // User, via logger methods.  Passed to actor in Log Message

  val KIND = "kind" // in Log message, but never set?

  val METHOD     = "method"     // this and items below are used and populated
  val TRACE      = "trace"      // iff an ex has been passed to actor.
  val STACK      = "stack"      // these values are determined from ex throwable
  val PLAINSTACK = "plainstack" // these values are determined from ex throwable
  val CAUSE      = "CAUSE"      // see above
}
