package glint

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Provides some basic functions (e.g. setup of client, server and master) when performing system-wide tests
  */
trait SystemTest extends ScalaFutures {

  val testConfig = ConfigFactory.parseString(
    """
      |glint {
      |  master {
      |    host = "127.0.0.1"
      |    port = 13370
      |    name = "master"
      |    system = "glint-master"
      |    startup-timeout = 30 seconds
      |    akka = ${glint.default.akka}
      |    akka.remote {
      |      log-remote-lifecycle-events = on
      |      netty.tcp {
      |        hostname = ${glint.master.host}
      |        port = ${glint.master.port}
      |      }
      |    }
      |  }
      |  server {
      |    system = "glint-server"
      |    name = "server"
      |    registration-timeout = 10 seconds
      |    akka = ${glint.default.akka}
      |    akka.remote.netty.tcp.hostname = "127.0.0.1"
      |    akka.remote.netty.tcp.port = 0
      |  }
      |  client {
      |    host = "127.0.0.1"
      |    port = 0
      |    system = "glint-client"
      |    timeout = 30 seconds
      |    akka = ${glint.default.akka}
      |    akka.remote.netty.tcp {
      |      hostname = ${glint.client.host}
      |      port = ${glint.client.port}
      |    }
      |  }
      |  default {
      |    akka {
      |      event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
      |      loglevel = "OFF"
      |      stdout-loglevel = "OFF"
      |      remote {
      |        log-remote-lifecycle-events = off
      |        enable-transports = ["akka.remote.netty.tcp"]
      |        netty.tcp {
      |          maximum-frame-size = 1280000b
      |        }
      |      }
      |      actor {
      |        provider = "akka.remote.RemoteActorRefProvider"
      |        serializers {
      |          java = "akka.serialization.JavaSerializer"
      |          requestserializer = "glint.serialization.RequestSerializer"
      |          responseserializer = "glint.serialization.ResponseSerializer"
      |        }
      |        serialization-bindings {
      |          "glint.messages.server.request.PullMatrix" = requestserializer
      |          "glint.messages.server.request.PullMatrixRows" = requestserializer
      |          "glint.messages.server.request.PullVector" = requestserializer
      |          "glint.messages.server.request.PushMatrixDouble" = requestserializer
      |          "glint.messages.server.request.PushMatrixFloat" = requestserializer
      |          "glint.messages.server.request.PushMatrixInt" = requestserializer
      |          "glint.messages.server.request.PushMatrixLong" = requestserializer
      |          "glint.messages.server.request.PushVectorDouble" = requestserializer
      |          "glint.messages.server.request.PushVectorFloat" = requestserializer
      |          "glint.messages.server.request.PushVectorInt" = requestserializer
      |          "glint.messages.server.request.PushVectorLong" = requestserializer
      |          "glint.messages.server.response.ResponseDouble" = responseserializer
      |          "glint.messages.server.response.ResponseFloat" = responseserializer
      |          "glint.messages.server.response.ResponseInt" = responseserializer
      |          "glint.messages.server.response.ResponseLong" = responseserializer
      |        }
      |      }
      |    }
      |  }
      |}
    """.stripMargin
  ).resolve()

  implicit val ec = ExecutionContext.Implicits.global

  implicit val timeout = Timeout(10 seconds)

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
    * Fixture that starts multiple parameter servers when running test code and cleans up where necessary
    *
    * @param nrOfServers The number of servers to start
    * @param testCode The test code to run
    */
  def withServers(nrOfServers: Int)(testCode: Seq[ActorRef] => Any): Unit = {
    val servers = (0 until nrOfServers).map {
      case i => whenReady(Server.run(testConfig)) { case (s, a) => (s, a) }
    }
    try {
      testCode(servers.map { case (s, a) => a })
    } finally {
      servers.foreach {
        case (s, a) =>
          s.shutdown()
          s.awaitTermination()
      }
    }

  }

  /**
    * Fixture that starts a client when running test code and cleans up afterwards
    *
    * @param testCode The test code to run
    */
  def withClient(testCode: Client => Any): Unit = {
    val client = Client(testConfig)
    try {
      testCode(client)
    } finally {
      client.stop()
    }
  }

}
