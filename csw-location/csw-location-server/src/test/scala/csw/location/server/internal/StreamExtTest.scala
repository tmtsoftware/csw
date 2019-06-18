package csw.location.server.internal

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.StreamExt.RichSource
import org.scalatest.{FunSuite, Matchers}

class StreamExtTest extends FunSuite with Matchers {
  private implicit val mat: ActorMaterializer = ActorMaterializer()(ActorSystem(Behaviors.empty, "test"))

  test("distinctUntilChanged") {
    Source(List(1, 2, 2, 3, 3, 3, 4, 1, 5, 5, 5, 4, 3, 4)).distinctUntilChanged.runWith(Sink.seq).await shouldBe List(
      1, 2, 3, 4, 1, 5, 4, 3, 4
    )
  }

}
