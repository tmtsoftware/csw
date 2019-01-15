package csw.logging

/**
 * == Logging Service ==
 *
 * This is a Actor based logging library which supports writing logs to File and StdOut Console.
 *
 * === Features ===
 *
 *    - Supports component specific log levels, ex. HCD1 can choose to log at `info` level and HCD2 can choose to log at `debug` level
 *    - Supports dynamically changing component log levels
 *    - Asynchronous thread safe logging
 *    - Structured logging
 *    - Supports overriding default logging properties per component viz
 *    - Intercepting logs from akka/slf4j
 *    - Supports JSON logging
 *
 * === LogActor ===
 *
 * `LogActor` is the heart of logging library. It receives messages from following classes:
 *
 *  - Slf4jAppender: Intercepts Slf4j logs and forwards it to LogActor via MessageHandler
 *  - AkkaLogger: Intercepts Akka logs and forwards it to LogActor via MessageHandler.
 *  - LoggerImpl: Provides csw logging API for component writer to log messages which gets forwarded to LogActor via MessageHandler
 *
 * === Logging Appenders ===
 *
 * This library supports two types of [[csw.logging.core.appenders.LogAppender]]:
 *  - [[csw.logging.core.appenders.FileAppender]]:
 *   Common log messages are written in Pretty Json form.
 *   This appender is useful during development. It is recommended to disable it in production.
 *
 *  - [[csw.logging.core.appenders.StdOutAppender]]:
 *  Log messages are written as Json, one per line.
 *  Ordinary log messages are written to the common log files. Each log file includes a day as part of its name. Each day a new file is created.
 *
 * You can specify the appender in application.conf file as shown below:
 * {{{
 *
 *   csw-logging {
 *      appenders = ["csw.logging.core.appenders.StdOutAppender$", "csw.logging.core.appenders.FileAppender$"]
 *    }
 *
 * }}}
 *
 * === Component Specific Log Levels ===
 *
 * For each component, `ComponentLoggingState` instance gets created which maintains log levels
 * which are enabled and disabled for this particular component.
 * Every time message gets logged by component, LoggerImpl checks in corresponding componentLoggingState whether current log level enabled or not.
 * If enabled, then only log message gets forwarded to LogActor via MessageHandler.
 *
 * You can specify the component specific log levels in application.conf file as shown below:
 *
 * {{{
 *
 *   component-log-levels {
 *      tromboneHcd = debug
 *      tromboneAssembly = error
 *   }
 *
 * }}}
 *̄
 * Detailed documentation of Logging Service is available at:
 * [[https://tmtsoftware.github.io/csw/services/logging.html]]
 */
package object core {

  /**
   * The type for rich messages.
   * This can be a String or Map[String,String]
   * See the project README file for other options.
   *
   */
  /**
   * Marker to indicate no exception is present
   */
  val NoLogException = new Exception("No Log Exception")
}
