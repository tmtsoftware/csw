package csw.aas.react4s

import com.github.ahnfelt.react4s.Component
import csw.aas.react4s.components.MainComponent
import csw.aas.react4s.facade.NpmReactBridge

object Main {
  def main(arguments: Array[String]): Unit = {
    NpmReactBridge.renderToDomById(Component(MainComponent), "main")
  }
}
