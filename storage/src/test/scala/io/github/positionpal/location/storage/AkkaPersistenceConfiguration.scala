package io.github.positionpal.location.storage

/** A provider of Akka Persistence configuration, for testing purposes. */
object AkkaPersistenceConfiguration:

  import com.typesafe.config.Config
  import com.typesafe.config.ConfigFactory

  val get: Config = ConfigFactory.parseString("""
      |akka {
      |  persistence {
      |    journal.plugin = "akka.persistence.cassandra.journal"
      |    snapshot-store.plugin = "akka.persistence.cassandra.snapshot"
      |    journal.auto-start-journals = ["akka.persistence.cassandra.journal"]
      |    cassandra {
      |      events-by-tag {
      |        bucket-size = "Day"
      |        eventual-consistency-delay = 200 ms
      |        flush-interval = 50ms
      |      }
      |      query {
      |        refresh-interval = 1s
      |      }
      |      journal.keyspace = "locationservice"
      |      snapshot.keyspace = "locationservice"
      |    }
      |  }
      |  projection {
      |    cassandra.offset-store.keyspace = "locationservice"
      |    cassandra.session-config-path = "akka.persistence.cassandra"
      |  }
      |}
      |datastax-java-driver {
      |  advanced.reconnect-on-init = on
      |}
      |""".stripMargin)
