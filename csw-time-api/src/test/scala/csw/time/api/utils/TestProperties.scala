package csw.time.api.utils

trait TestProperties {
  def precision: Int
}

object JTestProperties {
  class LinuxProperties extends TestProperties {
    override val precision: Int = 9
  }

  class NonLinuxProperties extends TestProperties {
    override val precision: Int = 3
  }

  val instance: TestProperties = OSType.value match {
    case OSType.Linux => new LinuxProperties
    case OSType.Other => new NonLinuxProperties
  }
}
