import $ivy.`com.typesafe.akka::akka-actor-typed:2.5.11`
import $ivy.`com.typesafe.akka::akka-cluster-tools:2.5.11`

import $ivy.`com.github.tmtsoftware.csw::csw-framework:0.1-SNAPSHOT`

import java.io.File

import ammonite.ops._
import ammonite.ops.PathConvertible.StringConvertible
import com.typesafe.config.ConfigFactory
import csw.framework.deploy.containercmd.ContainerCmd


@main
def main(importScript: String, containerConfig: String) = {

  def tempPath = StringConvertible(importScript)

  val importScriptPath = {
    if (tempPath.isAbsolute) {
      Path(tempPath)
    } else {
      pwd / importScript
    }
  }

  interp.load.module(importScriptPath)

  val defaultConfig = ConfigFactory.parseFile(new File(containerConfig))
  ContainerCmd.start(defaultConfig.getString("name"), Array[String](), Some(defaultConfig))
}

