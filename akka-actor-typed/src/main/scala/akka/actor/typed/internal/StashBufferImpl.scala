/*
 * Copyright (C) 2018-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.actor.typed.internal

import java.util.function.Consumer
import java.util.function.{ Function ⇒ JFunction }

import akka.actor.typed.Behavior
import akka.actor.typed.Behavior.DeferredBehavior
import akka.actor.typed.ExtensibleBehavior
import akka.actor.typed.PostStop
import akka.actor.typed.PreRestart
import akka.actor.typed.Signal
import akka.actor.typed.TypedActorContext
import akka.actor.typed.javadsl
import akka.actor.typed.scaladsl
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.annotation.InternalApi
import akka.util.ConstantFun

import scala.annotation.tailrec

/**
 * INTERNAL API
 */
@InternalApi private[akka] object StashBufferImpl {
  private final class Node[T](var next: Node[T], val message: T) {
    def apply(f: T ⇒ Unit): Unit = f(message)
  }

  def apply[T](capacity: Int): StashBufferImpl[T] =
    new StashBufferImpl(capacity, null, null)
}

/**
 * INTERNAL API
 */
@InternalApi private[akka] final class StashBufferImpl[T] private (
  val capacity:       Int,
  private var _first: StashBufferImpl.Node[T],
  private var _last:  StashBufferImpl.Node[T])
  extends javadsl.StashBuffer[T] with scaladsl.StashBuffer[T] {

  import StashBufferImpl.Node

  private var _size: Int = if (_first eq null) 0 else 1

  override def isEmpty: Boolean = _first eq null

  override def nonEmpty: Boolean = !isEmpty

  override def size: Int = _size

  override def isFull: Boolean = _size == capacity

  override def stash(message: T): StashBufferImpl[T] = {
    if (message == null) throw new NullPointerException
    if (isFull)
      throw new javadsl.StashOverflowException(s"Couldn't add [${message.getClass.getName}] " +
        s"because stash with capacity [$capacity] is full")

    val node = new Node(null, message)
    if (isEmpty) {
      _first = node
      _last = node
    } else {
      _last.next = node
      _last = node
    }
    _size += 1
    this
  }

  private def dropHead(): T = {
    val message = head
    _first = _first.next
    _size -= 1
    if (isEmpty)
      _last = null

    message
  }

  override def head: T =
    if (nonEmpty) _first.message
    else throw new NoSuchElementException("head of empty buffer")

  override def foreach(f: T ⇒ Unit): Unit = {
    var node = _first
    while (node ne null) {
      node(f)
      node = node.next
    }
  }

  override def forEach(f: Consumer[T]): Unit = foreach(f.accept(_))

  override def unstashAll(ctx: scaladsl.ActorContext[T], behavior: Behavior[T]): Behavior[T] =
    unstash(ctx, behavior, size, ConstantFun.scalaIdentityFunction[T])

  override def unstashAll(ctx: javadsl.ActorContext[T], behavior: Behavior[T]): Behavior[T] =
    unstashAll(ctx.asScala, behavior)

  override def unstash(ctx: scaladsl.ActorContext[T], behavior: Behavior[T],
                       numberOfMessages: Int, wrap: T ⇒ T): Behavior[T] = {
    val iter = new Iterator[T] {
      override def hasNext: Boolean = StashBufferImpl.this.nonEmpty
      override def next(): T = wrap(StashBufferImpl.this.dropHead())
    }.take(numberOfMessages)
    new UnstashingBehavior[T](behavior, ctx, iter)
  }

  private final class UnstashingBehavior[T](behavior: Behavior[T], ctx: TypedActorContext[T], messages: Iterator[T]) extends DeferredBehavior[T] with ExtensibleBehavior[T] {
    println("stashbuffer created unstashing behavior")
    var lastBehavior = behavior

    override def apply(ctx: TypedActorContext[T]): Behavior[T] = {
      println("stashbuffer starting unstashing behavior")
      @tailrec def interpretOne(b: Behavior[T]): Behavior[T] = {
        val b2 = Behavior.start(b, ctx)
        if (!Behavior.isAlive(b2) || !messages.hasNext) b2
        else {
          val nextMessage = messages.next()
          println(s"stashbuffer unstashed $nextMessage, passing it to $lastBehavior")
          val nextB = nextMessage match {
            case sig: Signal ⇒ Behavior.interpretSignal(b2, ctx, sig)
            case msg         ⇒ Behavior.interpretMessage(b2, ctx, msg)
          }
          lastBehavior = Behavior.start(Behavior.canonicalize(nextB, b, ctx), ctx)
          interpretOne(lastBehavior) // recursive
        }
      }

      interpretOne(Behavior.start(behavior, ctx))
    }

    override def receive(ctx: TypedActorContext[T], msg: T): Behavior[T] =
      throw new IllegalStateException("Unstashing behavior should never receive any messages")

    override def receiveSignal(ctx: TypedActorContext[T], msg: Signal): Behavior[T] = {
      // pass on the restart or stop signals to the last unstashed behavior
      msg match {
        case PreRestart | PostStop ⇒
          Behavior.interpretSignal(lastBehavior, ctx, msg)
        case _ ⇒
          throw new IllegalStateException(s"Unstashing behavior should never receive any other signals than PreRestart or PostStop, got $msg")
      }
    }
  }

  override def unstash(ctx: javadsl.ActorContext[T], behavior: Behavior[T],
                       numberOfMessages: Int, wrap: JFunction[T, T]): Behavior[T] =
    unstash(ctx.asScala, behavior, numberOfMessages, x ⇒ wrap.apply(x))

  override def toString: String =
    s"StashBuffer($size/$capacity)"
}

