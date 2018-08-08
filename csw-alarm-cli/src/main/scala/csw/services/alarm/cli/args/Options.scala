package csw.services.alarm.cli.args
import java.nio.file.Path

case class Options(
    cmd: String = "",
    filePath: Option[Path] = None,
    isLocal: Boolean = false,
    reset: Boolean = false
)
