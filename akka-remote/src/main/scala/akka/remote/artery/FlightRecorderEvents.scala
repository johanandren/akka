/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.remote.artery

import java.net.InetSocketAddress

import akka.actor.Address
import akka.remote.UniqueAddress
import jdk.jfr._
import JFREventUtils.stringOf

// requires JDK 9+
object JFREventUtils {
  def stringOf(address: Address): String =
    s"${address.host.get}:${address.port.get}"

  def stringOf(address: InetSocketAddress): String =
    s"${address.getHostString}:${address.getPort}"

}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Started")
class TransportStarted extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Unique Adress Set")
class TransportUniqueAddressSet(addr: UniqueAddress) extends Event {
  @Label("Address")
  val uniqueAddress = addr.toString()
}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Materializer Started ")
class TransportMaterializerStarted extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Startup Finished")
class TransportStartupFinished extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Restart Inbound")
class TransportRestartInbound(addr: UniqueAddress, val streamName: String) extends Event {
  val local = addr.toString
}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport KillSwitch Pulled")
class TransportKillSwitchPulled extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Send Queue Overflow")
class TransportSendQueueOverflow(val queueIndex: Int) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Quarantined")
class TransportQuarantined(remoteAddress: Address, val uid: Long) extends Event {
  val remote = stringOf(remoteAddress)
}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Removed Quarantined")
class TransportRemovedQuarantined(remoteAddress: Address) extends Event {
  val remote = stringOf(remoteAddress)
}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Stop Idle Outbound")
class TransportStopIdleOutbound(remoteAddress: Address, val queueIndex: Int) extends Event {
  val remote = stringOf(remoteAddress)
}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Restart Outbound")
class TransportRestartOutbound(remoteAddress: Address, val streamName: String) extends Event {
  val remote = stringOf(remoteAddress)
}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Started")
class AeronSinkStarted(val channel: String) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Task Runner Removed")
class AeronSinkTaskRunnerRemoved(val channel: String) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Publication Closed")
class AeronSinkPublicationClosed(val channel: String) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Stopped")
class AeronSinkStopped(val channel: String) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Envelope Grabbed")
class AeronSinkEnvelopeGrabbed(val lastMessageSize: Int) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Delegate to TaskRunner")
class AeronSinkDelegateToTaskRunner(val counfBeforeDelegate: Long) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Return from TaskRunner")
class AeronSinkReturnFromTaskRunner(@Timespan(Timespan.MILLISECONDS) val timing: Long) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Envelope Offered")
class AeronSinkEnvelopeOffered(val lastMessageSize: Int) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Gave Up Envelope")
class AeronSinkGaveUpEnvelope(val cause: String) extends Event // FIXME these two are alert, can we mark as more important somehow?

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Sink Unexpected Publication Closed")
class AeronSinkUnexpectedPublicationClosed(val channel: String) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Source Started")
class AeronSourceStarted(val channel: String) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Source Stopped")
class AeronSourceStopped(val channel: String) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Source Delegate to TaskRunner")
class AeronSourceDelegateToTaskRunner(val countBeforeDelegate: Long) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Source Return from TaskRunner")
class AeronSourceReturnFromTaskRunner(val timing: Long) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Aeron Source Received")
class AeronSourceReceived(val size: Int) extends Event

// Aeron UDP
@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Aeron Error Log Started")
class TransportAeronErrorLogStarted extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport TaskRunner Started")
class TransportTaskRunnerStarted extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport MediaDriver Started")
class TransportMediaDriverStarted(val aeronDirectoryName: String) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport MediaFile Deleted")
class TransportMediaFileDeleted extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Stopped")
class TransportStopped extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("Transport Aeron Error Log Task Stopped")
class TransportAeronErrorLogTaskStopped extends Event

// artery tcp
@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("TCP Outbound Connected")
class TcpOutboundConnected(remoteAddress: Address, val streamName: String) extends Event {
  val remote = stringOf(remoteAddress)
}

// lower level
@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("TCP Inbound Received")
class TcpInboundReceived(val size: Int) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("TCP Outbound Sent")
class TcpOutboundSent(val size: Int) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("TCP Inbound Connected")
class TcpInboundConnected(remoteAddress: InetSocketAddress) extends Event {
  val remote = stringOf(remoteAddress)
}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("TCP Inbound Bound")
class TcpInboundBound(host: String, port: Int) extends Event {
  val local = s"$host:$port"
}

@StackTrace(false)
@Category(Array("Akka", "Remoting"))
@Label("TCP Inbound Unbound")
class TcpInboundUnbound(localAddress: Address) extends Event {
  val local = stringOf(localAddress)
}

// compression
@StackTrace(false)
@Category(Array("Akka", "Remoting", "Compression"))
@Label("Inbound Run ActorRef Advertisement")
class CompressionInboundRunActorRefAdvertisement(val originUid: Long) extends Event

@StackTrace(false)
@Category(Array("Akka", "Remoting", "Compression"))
@Label("Inbound Run ClassManifest Advertisement")
class CompressionInboundRunClassManifestAdvertisement(val originUid: Long) extends Event
