package csw.services.config.internal

object Main {
  private val wiring = new Wiring
  def main(args: Array[String]): Unit = {
    wiring.configServiceApp.startServer("localhost", 8080)
  }
}
