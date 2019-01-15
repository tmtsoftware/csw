package csw.event.cli.utils

import play.api.libs.json.{JsObject, JsValue, Json}

case class JsonSelector(paths: List[List[String]]) {
  def transform(json: JsObject): JsObject = ParamSetJson(json, Nil).select.json

  private case class ParamSetJson(json: JsObject, parents: List[String]) {
    def paramSet: Seq[ParamJson] = json("paramSet").as[Seq[JsObject]].map(x => ParamJson(x, parents))

    def select: ParamSetJson = paths match {
      case Nil ⇒ this
      case _ ⇒
        val selectedSet = paramSet.filter(_.matchesPartialPath).map(_.select)
        copy(json ++ Json.obj("paramSet" -> selectedSet.map(_.json)))
    }
  }

  private case class ParamJson(json: JsObject, parents: List[String]) {
    def matchesPartialPath: Boolean      = paths.exists(_.startsWith(path))
    private def matchesFullPath: Boolean = paths.contains(path)

    def select: ParamJson = {
      val selectedSets     = if (matchesFullPath) paramSets else paramSets.map(_.select)
      val selectedSetsJson = selectedSets.filter(_.paramSet.nonEmpty).map(_.json)
      copy(json ++ Json.obj("values" -> (selectedSetsJson ++ simpleValues)))
    }

    private def values: Seq[JsValue]         = json("values").as[Seq[JsValue]]
    private def simpleValues: Seq[JsValue]   = values.filterNot(_.isInstanceOf[JsObject])
    private def paramSets: Seq[ParamSetJson] = values.collect({ case x: JsObject => ParamSetJson(x, path) })

    private def path: List[String] = parents :+ json("keyName").as[String]
  }
}
