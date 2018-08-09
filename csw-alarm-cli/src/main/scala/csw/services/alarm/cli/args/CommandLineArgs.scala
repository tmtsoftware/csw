package csw.services.alarm.cli.args
import java.nio.file.Path

case class CommandLineArgs(
    cmd: String = "",
    filePath: Option[Path] = None,
    isLocal: Boolean = false,
    reset: Boolean = false
)
