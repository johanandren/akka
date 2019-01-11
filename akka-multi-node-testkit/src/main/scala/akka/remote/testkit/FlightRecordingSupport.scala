/*
 * Copyright (C) 2016-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.remote.testkit

import java.nio.file.{ Files, Path }
import java.util.UUID

import jdk.jfr.Recording
import jdk.jfr.consumer.RecordingFile

import scala.collection.JavaConverters._
/**
 * FIXME some kind of replacement for this with JFR
 *
 * Provides test framework agnostic methods to dump the Java Flight Recorder data after a test has completed - you
 * must integrate the logic with the testing tool you use yourself, calling `startFlightRecorder` on test start and
 * stopping/dumping it after running/when failing with `stopFlightRecorder`
 */
trait FlightRecordingSupport { self: MultiNodeSpec ⇒

  private var recording: Option[Recording] = None

  private lazy val automaticFlightRecorderPath =
    Files.createTempFile(getClass.getName + UUID.randomUUID().toString, ".jfr")

  def flightRecordingPath: Path = automaticFlightRecorderPath

  def startFlightRecorder(): Unit = {
    println("Starting flight recorder")
    val rec = new Recording()
    rec.setName(getClass.getName)
    rec.start()
    recording = Some(rec)
  }

  def stopFlightRecorder(dumpRecording: Boolean, printToStdOut: Boolean): Unit = {
    recording match {
      case Some(rec) ⇒
        rec.stop()
        if (dumpRecording || printToStdOut) {
          println(s"Dumping flight recording to $flightRecordingPath")
          rec.dump(flightRecordingPath)
          if (printToStdOut) {
            printFlightRecording(flightRecordingPath)
          }
        }
        rec.close()
      case None ⇒
        throw new IllegalStateException("stopFlightRecorder called without first starting the flight recorder")
    }

  }

  /**
   * Dump the contents of the flight recorder file to standard output
   */
  final protected def printFlightRecording(path: Path): Unit = {
    // use stdout/println as we do not know if the system log is alive
    println(s"Flight recorder dump")
    RecordingFile.readAllEvents(path).asScala.foreach { event ⇒
      print(s"${event.getStartTime} ${event.getEventType.getLabel}: ") // FIXME enumerate the properties somehow?
      println(event.getFields.asScala.flatMap { valueDescriptor ⇒
        valueDescriptor.getLabel match {
          case "Duration" ⇒
            if (!event.getDuration().isZero) Some(s"duration=${event.getDuration}")
            else None

          case "Stack Trace"  ⇒ None
          case "Event Thread" ⇒ None // FIXME extract thread name
          case "Start Time"   ⇒ None
          case other ⇒
            val name = if (other != null) other else valueDescriptor.getName
            Some(s"$name=${event.getValue(valueDescriptor.getName)}")
        }
      }.mkString("[", ", ", "]"))
    }
  }

  private def destinationIsValidForDump(path: Path): Boolean = {
    val name = path.getFileName.toString
    name != "" && name.endsWith(".jfr")
  }

}
