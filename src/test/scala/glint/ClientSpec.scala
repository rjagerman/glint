package glint

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import breeze.linalg.DenseVector
import com.typesafe.config.ConfigFactory
import glint.messages.master.ClientList
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import spire.implicits._
import spire.algebra._

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
    |      loglevel = "DEBUG"
    |      stdout-loglevel = "DEBUG"
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
    |      loglevel = "DEBUG"
    |      stdout-loglevel = "DEBUG"
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
    |      loglevel = "DEBUG"
    |      stdout-loglevel = "DEBUG"
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

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(50, Millis))



  "A client" should "register with master" in {

    // Start basic set up
    val (masterSystem, masterActor) = setupMaster()

    try {

      // Create client and get remoting address
      val client = whenReady(Client(testConfig)) { case c => c }
      val clientAddress = client.system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress

      // Get client list from master
      implicit val timeout = Timeout(30 seconds)
      val clientList = whenReady(masterActor ? ClientList()) {
        case list: Array[ActorRef] => list
      }

      // Check for the existence of the address
      assert(clientList.exists(l =>
        l.path.toSerializationFormat == client.actor.path.toSerializationFormatWithAddress(clientAddress)))

    } finally {

      // Cleanup
      masterSystem.shutdown()
      masterSystem.awaitTermination()

    }

  }

  it should "register a created model with master" in {

    // Start basic set up
    val (masterSystem, _) = setupMaster()
    val (paramSystem, _) = setupParameterServer()

    try {

      // Create client
      val client = whenReady(Client(testConfig)) { case c => c }

      // Construct big model on parameter server
      val bigModel = whenReady(client.denseScalarModel[Double]("test", 100, 0.0)) { case a => a }

      // Obtain model reference from master
      val bigModel2 = whenReady(client.get[Long, Double]("test")) { case a => a }

      // Check for the existence of the model
      assert(bigModel2.nonEmpty)

    } finally {

      // Cleanup
      masterSystem.shutdown()
      paramSystem.shutdown()
      masterSystem.awaitTermination()
      paramSystem.awaitTermination()

    }

  }

  it should "store Double values in a scalar model" in {

    // Start basic set up
    val (masterSystem, _) = setupMaster()
    val (paramSystem, _) = setupParameterServer()

    try {

      // Create client
      val client = whenReady(Client(testConfig)) { case c => c }

      // Construct big model on parameter server
      val bigModel = whenReady(client.denseScalarModel[Double]("test", 100, 0.3)) { case a => a }

      whenReady(bigModel.pushSingle(20, 0.5)) { case p => p }
      assert(whenReady(bigModel.pullSingle(20)) {case a => a} == 0.8) // 0.3 + 0.5 = 0.8

    } finally {

      // Cleanup
      masterSystem.shutdown()
      paramSystem.shutdown()

    }

  }

  it should "store Int values in a scalar model" in {

    // Start basic set up
    val (masterSystem, _) = setupMaster()
    val (paramSystem, _) = setupParameterServer()

    try {

      // Create client
      val client = whenReady(Client(testConfig)) { case c => c }

      // Construct big model on parameter server
      val bigModel = whenReady(client.denseScalarModel[Int]("test", 100, 5)) { case a => a }

      whenReady(bigModel.pushSingle(20, 3)) { case p => p }
      whenReady(bigModel.pushSingle(76, -100)) { case p => p }
      assert(whenReady(bigModel.pullSingle(20)) {case a => a} == 8) // 5 + 3 = 8
      assert(whenReady(bigModel.pullSingle(76)) {case a => a} == -95) // 5 + -100 = -95

    } finally {

      // Cleanup
      masterSystem.shutdown()
      paramSystem.shutdown()

    }

  }

  it should "store DenseVector[Double] values a vector model" in {

    // Start basic set up
    val (masterSystem, _) = setupMaster()
    val (paramSystem, _) = setupParameterServer()

    try {

      // Create client
      val client = whenReady(Client(testConfig)) { case c => c }

      // Construct big model on parameter server
      val bigModel = whenReady(client.denseVectorModel[Double]("test", 100, DenseVector.ones[Double](10))) { case a => a }

      // Construct difference that will be send to parameter server
      val difference = DenseVector.zeros[Double](10)
      difference(3) = 0.9
      difference(7) = -0.3

      // Compute expected result
      val expected = DenseVector.ones[Double](10) + difference

      // Push to server
      whenReady(bigModel.pushSingle(20, difference)) { case p => p }

      // Pull after push is done, verifying the expected result
      assert(whenReady(bigModel.pullSingle(20)) { case a => a } == expected)

    } finally {

      // Cleanup
      masterSystem.shutdown()
      paramSystem.shutdown()

    }

  }

  it should "store DenseVector[Int] values a vector model" in {

    // Start basic set up
    val (masterSystem, _) = setupMaster()
    val (paramSystem, _) = setupParameterServer()

    try {

      // Create client
      val client = whenReady(Client(testConfig)) { case c => c }

      // Construct big model on parameter server
      val bigModel = whenReady(client.denseVectorModel[Int]("test", 100, DenseVector.ones[Int](10))) { case a => a }

      // Construct difference that will be send to parameter server
      val difference = DenseVector.zeros[Int](10)
      difference(3) = 8
      difference(7) = -3

      // Compute expected result
      val expected = DenseVector.ones[Int](10) + difference

      // Push to server
      whenReady(bigModel.pushSingle(42, difference)) { case p => p }

      // Pull after push is done, verifying the expected result
      assert(whenReady(bigModel.pullSingle(42)) { case a => a } == expected)

    } finally {

      // Cleanup
      masterSystem.shutdown()
      paramSystem.shutdown()

    }

  }

  private def setupMaster(): (ActorSystem, ActorRef) = {
    whenReady(Master.run(testConfig)) { case (s,a) => (s,a) }
  }

  private def setupParameterServer(): (ActorSystem, ActorRef) = {
    whenReady(Server.run(testConfig, "localhost", 0)) { case (s,a) => (s,a) }
  }


}
