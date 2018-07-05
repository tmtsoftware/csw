package csw.services.event.cli

import ujson.Js

case class ParamSetJson(json: Js.Obj, parents: List[String]) {
  val params: Seq[ParamJson] = json("paramSet").arr.map(x => ParamJson(x.obj, parents))

  def prune(paths: List[List[String]]): Unit = {
    paths match {
      case Nil ⇒
      case _ ⇒
        json("paramSet") = params.collect {
          case param if paths.exists(_.startsWith(param.path)) => param.json
        }

        for {
          param <- params
          if !paths.contains(param.path)
          paramSet <- param.paramSets
        } {
          paramSet.prune(paths)
          param.prune()
        }
    }
  }
}
case class ParamJson(json: Js.Value, parents: List[String]) {
  def prune(): Unit = {
    json("values") = paramSets.collect {
      case paramSet if paramSet.params.nonEmpty => paramSet.json
    }
  }
  def keyName: String = json("keyName").str
  def paramSets: Seq[ParamSetJson] = json("values").arr.collect {
    case x: Js.Obj => ParamSetJson(x, path)
  }
  def path: List[String] = parents :+ keyName
}
