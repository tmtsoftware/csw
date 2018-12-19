package csw.database.commons

import java.io.InputStream
import java.lang.String.format

import com.opentable.db.postgres.embedded.PgBinaryResolver

class PostgresBinaryResolver extends PgBinaryResolver {
  override def getPgBinary(system: String, machineHardware: String): InputStream =
    Thread
      .currentThread()
      .getContextClassLoader
      .getResourceAsStream(format("postgresql-%s-%s.txz", system, machineHardware))
}
