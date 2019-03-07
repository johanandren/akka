/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.persistence.typed.javadsl

import akka.actor.typed.Signal
import akka.japi.function.Procedure
import akka.japi.function.{ Effect ⇒ JEffect }

object SignalHandler {
  val EmptySignalHandler = new SignalHandler
}

final class SignalHandler {

}

final class SignalHandlerBuilder {

  private var handler: PartialFunction[Signal, Unit] = PartialFunction.empty

  def onSignal[T <: Signal](signalType: Class[T], callback: Procedure[T]): SignalHandlerBuilder = {
    // FIXME uff
    ???
  }

  def onSignal[T <: Signal](signal: T, callback: JEffect): SignalHandlerBuilder = ???

  def build: SignalHandler = ???

}
