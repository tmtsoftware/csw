package csw.services.event.cli

import play.api.libs.json.{JsObject, JsValue, Json}

case class ParamSetJson2(json: JsObject, parents: List[String]) {
  def paramSet: Seq[ParamJson2] = json("paramSet").as[Seq[JsObject]].map(x => ParamJson2(x, parents))

  def prune(paths: List[List[String]]): ParamSetJson2 = paths match {
    case Nil ⇒ this
    case _ ⇒
      val prunedSet = paramSet
        .filter(param => paths.exists(_.startsWith(param.path)))
        .map(_.prune(paths))
      ParamSetJson2(json ++ Json.obj("paramSet" -> prunedSet.map(_.json)), parents)
  }
}
case class ParamJson2(json: JsObject, parents: List[String]) {
  def prune(paths: List[List[String]]): ParamJson2 = {
    val prunedSets   = if (!paths.contains(path)) paramSets.map(_.prune(paths)) else paramSets
    val prunedValues = prunedSets.filter(_.paramSet.nonEmpty).map(_.json) ++ simpleValues
    ParamJson2(json ++ Json.obj("values" -> prunedValues), parents)
  }
  ///////////////
  def values: Seq[JsValue] = json("values").as[Seq[JsValue]]
  def paramSets: Seq[ParamSetJson2] = values.collect {
    case x: JsObject => ParamSetJson2(x, path)
  }
  def simpleValues: Seq[JsValue] = values.filterNot(_.isInstanceOf[JsObject])
  /////////////
  def path: List[String] = parents :+ keyName
  def keyName: String    = json("keyName").as[String]
}
