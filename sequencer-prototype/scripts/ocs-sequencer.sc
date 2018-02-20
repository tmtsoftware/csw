import tmt.development.dsl.Dsl.wiring._

forEach { command =>
  if (command.name == "setup-assembly1") {
    println(cs.submit("assembly1", command))
  }
  else if (command.name == "setup-assembly2") {
    println(cs.submit("assembly2", command))
  }
  else if (command.name == "setup-assemblies-sequential") {
    val (params1, params2) = cs.split(command.params)
    println(cs.submit("assembly1", Command("setup-assembly1", params1)))
    println(cs.submit("assembly2", Command("setup-assembly2", params2)))
  }
  else if (command.name == "setup-assemblies-parallel") {
    val (params1, params2) = cs.split(command.params)
    val responses = par(
      cs.submit("assembly1", Command("setup-assembly1", params1)),
      cs.submit("assembly2", Command("setup-assembly2", params2))
    )
    println(responses)
  }
  else {
    println(s"unknown command=$command")
  }
}
