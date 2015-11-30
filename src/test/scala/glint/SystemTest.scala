package glint

import akka.actor.{ActorSystem, ActorRef}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.ExecutionContext

/**
  * Provides some basic functions (e.g. setup of client, server and master) when performing system-wide tests
  */
trait SystemTest extends ScalaFutures  {

  val testConfig = ConfigFactory.parseString(
    """
      |glint {
      |
      |  // Configuration for the master
      |  master {
      |    host = "127.0.0.1"
      |    port = 13370
      |    name = "master"
      |    system = "glint-master"
      |    startup-timeout = 30 seconds
      |    loglevel = "ERROR"
      |
      |    akka {
      |      event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
      |      loglevel = "ERROR"
      |      stdout-loglevel = "ERROR"
      |      actor {
      |        provider = "akka.remote.RemoteActorRefProvider"
      |      }
      |      remote {
      |        log-remote-lifecycle-events = off
      |        enable-transports = ["akka.remote.netty.tcp"]
      |        netty.tcp {
      |          hostname = ${glint.master.host}
      |          port = ${glint.master.port}
      |        }
      |      }
      |    }
      |
      |  }
      |
      |  // Configuration for the parameter servers
      |  server {
      |    memory = "2g"
      |    system = "glint-server"
      |    name = "server"
      |    registration-timeout = 10 seconds
      |    loglevel = "ERROR"
      |
      |    akka {
      |      event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
      |      loglevel = "ERROR"
      |      stdout-loglevel = "ERROR"
      |      actor {
      |        provider = "akka.remote.RemoteActorRefProvider"
      |      }
      |      remote {
      |        log-remote-lifecycle-events = off
      |        enable-transports = ["akka.remote.netty.tcp"]
      |        netty.tcp {
      |          port = 0
      |        }
      |      }
      |    }
      |  }
      |
      |  // Configuration for the clients (e.g. spark workers)
      |  client {
      |    host = "127.0.0.1"
      |    port = 0
      |    system = "glint-client"
      |    default-timeout = 5 seconds
      |    loglevel = "ERROR"
      |
      |    akka {
      |      event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
      |      loglevel = "ERROR"
      |      stdout-loglevel = "ERROR"
      |      actor {
      |        provider = "akka.remote.RemoteActorRefProvider"
      |      }
      |      remote {
      |        log-remote-lifecycle-events = off
      |        enable-transports = ["akka.remote.netty.tcp"]
      |        netty.tcp {
      |          hostname = ${glint.client.host}
      |          port = ${glint.client.port}
      |        }
      |      }
      |    }
      |  }
      |
      |}
    """.stripMargin
  ).resolve()

  implicit val ec = ExecutionContext.Implicits.global

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(50, Millis))

  /**
    * Fixture that starts a master when running test code and cleans up where necessary
    *
    * @param testCode The test code to run
    */
  def withMaster(testCode: ActorRef => Any): Unit = {
    val (masterSystem: ActorSystem, masterActor: ActorRef) = whenReady(Master.run(testConfig)) { case (s, a) => (s, a) }
    try {
      testCode(masterActor)
    } finally {
      masterSystem.shutdown()
      masterSystem.awaitTermination()
    }
  }

  /**
    * Fixture that starts a parameter server when running test code and cleans up where necessary
    *
    * @param testCode The test code to run
    */
  def withServer(testCode: ActorRef => Any): Unit = {
    val (serverSystem: ActorSystem, serverActor: ActorRef) = whenReady(Server.run(testConfig)) { case (s, a) => (s, a) }
    try {
      testCode(serverActor)
    } finally {
      serverSystem.shutdown()
      serverSystem.awaitTermination()
    }
  }

  /**
    * Fixture that starts a client when running test code and cleans up afterwards
    *
    * @param testCode The test code to run
    */
  def withClient(testCode: Client => Any): Unit = {
    val client = whenReady(Client(testConfig)) { case c => c }
    try {
      testCode(client)
    } finally {
      client.stop()
    }
  }

}
