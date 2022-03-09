/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream

import akka.Done
import akka.NotUsed
import akka.actor.ActorSystem
import akka.remote.artery.BenchTestSourceSameElement
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.RestartFlow
import akka.stream.scaladsl.RetryFlow
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class RetryFlowBenchmark {
  implicit val system: ActorSystem = ActorSystem("FlatMapMergeBenchmark")

  val NumberOfElements = 100000
  @Param(Array("none", "retry", "restart"))
  var WithRetry = ""

  var graph: RunnableGraph[Future[Done]] = _

  def createSource(count: Int): Graph[SourceShape[java.lang.Integer], NotUsed] =
    new BenchTestSourceSameElement(count, 1)

  @Setup
  def setup(): Unit = {
    val source = createSource(NumberOfElements)
    graph = WithRetry match {
      case "retry" =>
        Source
          .fromGraph(source)
          .via(RetryFlow.withBackoff(10.millis, 2.seconds, 1d, 20, Flow[Integer].map(_ + 1))((_, _) => None))
          .toMat(Sink.ignore)(Keep.right)
      case "restart" =>
        Source
          .fromGraph(source)
          .via(RestartFlow.withBackoff[Integer, Integer](RestartSettings(10.millis, 2.seconds, 1d))(() =>
            Flow[Integer].map(_ + 1)))
          .toMat(Sink.ignore)(Keep.right)
      case "none" =>
        Source.fromGraph(source).map(_ + 1).toMat(Sink.ignore)(Keep.right)
    }

    // eager init of materializer
    SystemMaterializer(system).materializer
  }

  @TearDown
  def shutdown(): Unit = {
    Await.result(system.terminate(), 5.seconds)
  }

  @Benchmark
  @OperationsPerInvocation(100000) // Note: needs to match NumberOfElements.
  def retry_flow_100k_elements(): Unit = {
    Await.result(graph.run(), Duration.Inf)
  }
}
