/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.api.internal

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.{Sink, Source}

import scala.concurrent.{Future, Promise}

/**
 * Extensions on pekko streams
 */
private[config] object ConfigStreamExts {

  /**
   * RichSource adds extra features on pekko streams
   *
   * @see [[org.apache.pekko.stream.scaladsl.Source]]
   * @tparam Out the type of values that will flow through this stream
   * @tparam Mat when the stream starts flowing, the handle to the Mat will be available
   */
  implicit class RichSource[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    /**
     * Takes up to `n` elements from the stream (less than `n` only if the upstream completes before emitting `n`
     * elements) and returns a pair containing a strict sequence of the taken element and the original stream
     *
     * @param n number of elements to be extracted as prefix
     */
    def prefixAndStitch(n: Int)(implicit system: ActorSystem[_]): (Future[Seq[Out]], Source[Out, Future[NotUsed]]) = {
      import system.executionContext
      val p = Promise[Seq[Out]]()
      val futureSource = source.prefixAndTail(n).runWith(Sink.head).map { case (prefix, remainingSource) =>
        p.success(prefix)
        Source(prefix) ++ remainingSource
      }
      (p.future, Source.futureSource(futureSource))
    }

  }
}
