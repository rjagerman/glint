package glint

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import glint.util.terminateAndWait

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Provides some basic functions (e.g. setup of client, server and master) when performing system-wide tests
  */
trait SystemTest extends ScalaFutures {

  val testConfig = ConfigFactory.load("glint")

  implicit val ec = ExecutionContext.Implicits.global

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(500, Millis))

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
      terminateAndWait(masterSystem, testConfig)
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
      terminateAndWait(serverSystem, testConfig)
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
          terminateAndWait(s, testConfig)
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
