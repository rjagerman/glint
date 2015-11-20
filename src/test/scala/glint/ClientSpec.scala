package glint

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import breeze.linalg.DenseVector
import com.typesafe.config.ConfigFactory
import glint.exceptions.ModelCreationException
import glint.messages.master.ClientList
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import spire.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Client test specification
  */
class ClientSpec extends FlatSpec with ScalaFutures {

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
      |
      |    akka {
      |      event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
      |      loglevel = "ERROR"
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
      |
      |    akka {
      |      event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
      |      loglevel = "ERROR"
      |      actor {
      |        provider = "akka.remote.RemoteActorRefProvider"
      |      }
      |      remote {
      |        log-remote-lifecycle-events = off
      |        enable-transports = ["akka.remote.netty.tcp"]
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
      |
      |    akka {
      |      event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
      |      loglevel = "ERROR"
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


  "A client" should "register with master" in withMaster { master =>
    withClient { client =>
      val clientAddress = client.system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress
      implicit val timeout = Timeout(30 seconds)
      val clientList = whenReady(master ? ClientList()) {
        case list: Array[ActorRef] => list
      }
      assert(clientList.exists(l =>
        l.path.toSerializationFormat == client.actor.path.toSerializationFormatWithAddress(clientAddress)))
    }
  }

  it should "register a created model with master" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Double]("test", 100, 0.0)) { case a => a }
        val bigModel2 = whenReady(client.get[Long, Double]("test")) { case a => a }
        assert(bigModel2.processing() == 0)
      }
    }
  }

  it should "fail to register a model when there are no servers" in withMaster { _ =>
    withClient { client =>
      whenReady(client.denseScalarModel("test", 100, 0.0).failed) {
        case e => e shouldBe a[ModelCreationException]
      }
    }
  }

  it should "store Double values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Double]("test", 100, 0.3)) { case a => a }
        whenReady(bigModel.pushSingle(20, 0.5)) { case p => p }
        assert(whenReady(bigModel.pullSingle(20)) { case a => a } == 0.8) // 0.3 + 0.5 = 0.8
      }
    }
  }

  it should "store Int values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Int]("test", 100, 5)) { case a => a }
        whenReady(bigModel.pushSingle(20, 3)) { case p => p }
        whenReady(bigModel.pushSingle(76, -100)) { case p => p }
        assert(whenReady(bigModel.pullSingle(20)) { case a => a } == 8) // 5 + 3 = 8
        assert(whenReady(bigModel.pullSingle(76)) { case a => a } == -95) // 5 + -100 = -95
      }
    }
  }

  it should "store Long values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Long]("test", 100, 8000000000L)) { case a => a }
        whenReady(bigModel.pushSingle(20, 3)) { case p => p }
        whenReady(bigModel.pushSingle(76, -100)) { case p => p }
        assert(whenReady(bigModel.pullSingle(20)) { case a => a } == 8000000003L) // 8000000000 + 3 = 8000000003
        assert(whenReady(bigModel.pullSingle(76)) { case a => a } == 7999999900L) // 8000000000 + -100 = 7999999900
      }
    }
  }

  it should "store DenseVector[Double] values a vector model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Double]("test", 100, DenseVector.ones[Double](10))) { case a => a }
        val difference = DenseVector.zeros[Double](10)
        difference(3) = 0.9
        difference(7) = -0.3
        val expected = DenseVector.ones[Double](10) + difference
        whenReady(bigModel.pushSingle(20, difference)) { case p => p }
        assert(whenReady(bigModel.pullSingle(20)) { case a => a } == expected)
      }
    }
  }

  it should "store DenseVector[Int] values a vector model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Int]("test", 100, DenseVector.ones[Int](10))) { case a => a }
        val difference = DenseVector.zeros[Int](10)
        difference(3) = 8
        difference(7) = -3
        val expected = DenseVector.ones[Int](10) + difference
        whenReady(bigModel.pushSingle(42, difference)) { case p => p }
        assert(whenReady(bigModel.pullSingle(42)) { case a => a } == expected)
      }
    }
  }

}
