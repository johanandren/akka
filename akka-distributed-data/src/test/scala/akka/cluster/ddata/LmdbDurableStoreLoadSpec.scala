/*
 * Copyright (C) 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.cluster.ddata

import akka.cluster.ddata.DurableStore.LoadData
import akka.testkit.AkkaSpec
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory

object LmdbDurableStoreLoadSpec {

  val Writes = 1000000
  val Keys = 1000

  val config = ConfigFactory.parseString(s"""
        akka.actor.provider=cluster
        lmdb {
          dir = target/LmdbDurableStoreLoadSpec-${System.currentTimeMillis}-ddata
          map-size = 100 MiB
          write-behind-interval = 100ms
        }
      """)

}

class LmdbDurableStoreLoadSpec extends AkkaSpec(LmdbDurableStoreLoadSpec.config) {

  import LmdbDurableStoreLoadSpec._

  "The LMDB durable store" must {

    "write a ton of data" in {
      val probe = TestProbe()
      val store = system.actorOf(LmdbDurableStore.props(system.settings.config))

      store.tell(DurableStore.LoadAll, probe.ref)
      probe.expectMsg(DurableStore.LoadAllCompleted)

      implicit val uniqueAddress = DistributedData(system).selfUniqueAddress

      var data = (0 until Keys).map(n => (n.toString -> GCounter.empty)).toMap

      for (n <- 1 to Writes) {
        val key = (n % Keys).toString
        val updated = data(key) :+ 1
        data = data.updated(key, updated)
        // this should just queue up each key for write behind, so we write once per key for each interval
        // to get many key writes in each batch
        store.tell(
          DurableStore.Store(
            key,
            new DurableStore.DurableDataEnvelope(updated),
            Some(DurableStore.StoreReply(s"ok-$n", s"fail-$n", probe.ref))),
          probe.ref)
        probe.expectMsg(s"ok-$n")
        if (n % Keys == 0) Thread.sleep(100)
        if (n % 100 == 0) print(".")
      }

      probe.watch(store)
      system.stop(store)
      probe.expectTerminated(store)

      // check that we can still load the data
      val storeincarnation2 = system.actorOf(LmdbDurableStore.props(system.settings.config))
      storeincarnation2.tell(DurableStore.LoadAll, probe.ref)
      probe.expectMsgType[LoadData].data.size should ===(Keys)
    }

  }

}
