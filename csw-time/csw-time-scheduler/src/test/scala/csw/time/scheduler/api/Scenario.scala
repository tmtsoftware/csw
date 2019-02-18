package csw.time.scheduler.api

case class Scenario(offset: Int, nSchedulers: Int, warmup: Int, nTasks: Int)

object TestSettings {

  // 1 KHz
  val oneMillis = List(
    Scenario(1, 1, 60 * 1000, 60 * 1000),
    Scenario(1, 5, 60 * 1000, 60 * 1000),
    Scenario(1, 10, 60 * 1000, 60 * 1000),
    Scenario(1, 20, 60 * 1000, 60 * 1000),
  )

  // 50 Hz
  val twentyMillis = List(
    Scenario(20, 1, 3 * 1000, 3 * 1000),
    Scenario(20, 5, 3 * 1000, 3 * 1000),
    Scenario(20, 10, 3 * 1000, 3 * 1000),
    Scenario(20, 20, 3 * 1000, 3 * 1000),
  )

  // 20 Hz
  val fiftyMillis = List(
    Scenario(50, 1, 1 * 1000, 1 * 1000),
    Scenario(50, 5, 1 * 1000, 1 * 1000),
    Scenario(50, 10, 1 * 1000, 1 * 1000),
    Scenario(50, 20, 1 * 1000, 1 * 1000)
  )

  // 10 Hz
  val hundredMillis = List(
    Scenario(100, 1, 1 * 1000, 1 * 1000),
    Scenario(100, 5, 1 * 1000, 1 * 1000),
    Scenario(100, 10, 1 * 1000, 1 * 1000),
    Scenario(100, 20, 1 * 1000, 1 * 1000)
  )

  val all: List[Scenario] = oneMillis ++ twentyMillis ++ fiftyMillis ++ hundredMillis
}
