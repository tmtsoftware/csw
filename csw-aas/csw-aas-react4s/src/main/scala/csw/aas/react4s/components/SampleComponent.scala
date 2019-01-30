package csw.aas.react4s.components
import com.github.ahnfelt.react4s._

case class SampleComponent() extends Component[NoEmit] {
  override def render(get: Get): Node = {
    E.div(Text("SampleComponent"))
  }
}
