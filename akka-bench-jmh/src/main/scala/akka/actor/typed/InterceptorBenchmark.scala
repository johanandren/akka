/*
 * Copyright (C) 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.actor.typed

import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.typed.BehaviorInterceptor.ReceiveTarget
import akka.actor.typed.scaladsl.Behaviors
import org.openjdk.jmh.annotations._

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

object InterceptorBenchmark {
  private val anyPassthroughInterceptor: BehaviorInterceptor[Any, Any] = new BehaviorInterceptor[Any, Any] {
    override def aroundReceive(
        ctx: TypedActorContext[Any],
        msg: Any,
        target: BehaviorInterceptor.ReceiveTarget[Any]): Behavior[Any] = {
      target(ctx, msg)
    }
  }

  private val subtypePassthroughInterceptor: BehaviorInterceptor[String, Any] = new BehaviorInterceptor[String, Any] {

    override val interceptMessageClass: Class[String] = classOf[String]

    override def aroundReceive(ctx: TypedActorContext[String], msg: String, target: ReceiveTarget[Any]): Behavior[Any] = {
      target(ctx, msg)
    }
  }

  case class Init(messages: Int, done: Promise[Done], replyTo: ActorRef[Done])

  def countingDeliveries(): Behavior[Any] = idle()

  private def idle(): Behavior[Any] = Behaviors.receive {
    case (_, Init(messages, done, replyTo)) =>
      replyTo ! Done
      running(messages, done)
    case (ctx, msg) =>
      ctx.log.warn("Unexpected message while idle: {}", msg)
      Behaviors.ignore
  }

  private def running(messages: Int, done: Promise[Done]): Behavior[Any] = Behaviors.setup { _ =>
    var messagesLeft = messages

    Behaviors.receiveMessage { _ =>
      messagesLeft -= 1

      if (messagesLeft > 0) Behaviors.same
      else {
        done.success(Done)
        idle()
      }
    }
  }

  def countingDeliveriesWithAnyPassthrough() =
    Behaviors.intercept(() => anyPassthroughInterceptor)(countingDeliveries())

  def countingDeliveriesWithSubtypePassthrough() =
    Behaviors.intercept(() => subtypePassthroughInterceptor)(countingDeliveries()).unsafeCast[Any]

  // this is probably the most interesting Any interceptor usage
  def countingDeliveriesSupervision() =
    Behaviors.supervise(countingDeliveries())
      .onFailure[RuntimeException](SupervisorStrategy.restart)

  // note that these will mostly be just dumped into the mailbox right away
  final val MessagesPerInvocation = 10000

}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@Fork(1)
@Threads(1)
@Warmup(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(iterations = 10, time = 15, timeUnit = TimeUnit.SECONDS, batchSize = 1)
class InterceptorBenchmark {

  import InterceptorBenchmark._

  private val system = ActorSystem(SpawnProtocol(), "InterceptorBenchmark")
  // pesky SLF4J init
  system.log.info("Starting up")

  private var actor: ActorRef[Any] = _
  private var done: Promise[Done] = _

  import akka.actor.typed.scaladsl.AskPattern._
  implicit val sc = system.scheduler
  implicit val timeout: akka.util.Timeout = 3.seconds

  @Param(Array("baseline", "anyPassthrough", "subtypePassthrough", "supervised"))
  var whichBench = ""

  @Setup(Level.Trial)
  def setup(): Unit = {
    val behavior = whichBench match {
      case "baseline" => countingDeliveries()
      case "anyPassthrough" => countingDeliveriesWithAnyPassthrough()
      case "subtypePassthrough" => countingDeliveriesWithSubtypePassthrough()
      case "supervised" => countingDeliveriesSupervision()
    }
    actor = Await.result(system.ask[ActorRef[Any]](SpawnProtocol.Spawn(behavior, "actor", Props.empty, _)), 3.seconds)
  }

  @Setup(Level.Invocation)
  def invocationSetup(): Unit = {
    done = Promise[Done]()
    Await.result(actor.ask[Done](Init(MessagesPerInvocation, done, _)), 3.seconds)
  }

  @TearDown(Level.Trial)
  def tearDown(): Unit = {
    system.terminate()
  }

  @Benchmark
  @OperationsPerInvocation(MessagesPerInvocation)
  def bench(): Unit = {
    var idx = MessagesPerInvocation
    val msg = "heyhey"
    while (idx > 0) {
      idx -= 1
      actor ! msg
    }
    Await.ready(done.future, 5.minutes)
  }

}
