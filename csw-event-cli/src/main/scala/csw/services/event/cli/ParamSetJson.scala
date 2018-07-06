package csw.services.event.cli

import play.api.libs.json.{JsObject, JsValue, Json}

case class ParamSetJson(json: JsObject, parents: List[String]) {
  def paramSet: Seq[ParamJson] = json("paramSet").as[Seq[JsObject]].map(x => ParamJson(x, parents))

  def prune(paths: List[List[String]]): ParamSetJson = paths match {
    case Nil ⇒ this
    case _ ⇒
      val prunedSet = paramSet
        .filter(param => paths.exists(_.startsWith(param.path)))
        .map(_.prune(paths))
      copy(json ++ Json.obj("paramSet" -> prunedSet.map(_.json)))
  }
}

case class ParamJson(json: JsObject, parents: List[String]) {
  def prune(paths: List[List[String]]): ParamJson = {
    val prunedSets   = if (!paths.contains(path)) paramSets.map(_.prune(paths)) else paramSets
    val prunedValues = prunedSets.filter(_.paramSet.nonEmpty).map(_.json) ++ simpleValues
    copy(json ++ Json.obj("values" -> prunedValues))
  }
  ///////////////
  def values: Seq[JsValue] = json("values").as[Seq[JsValue]]
  def paramSets: Seq[ParamSetJson] = values.collect {
    case x: JsObject => ParamSetJson(x, path)
  }
  def simpleValues: Seq[JsValue] = values.filterNot(_.isInstanceOf[JsObject])
  /////////////
  def path: List[String] = parents :+ json("keyName").as[String]
}
