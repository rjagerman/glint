package glint

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import com.typesafe.config.Config

package object util {

  /**
    * Terminates the actor system and waits for it to finish (or when it times out)
    *
    * @param system The actor system to terminate
    */
  def terminateAndWait(system: ActorSystem, config: Config)(implicit ec: ExecutionContext): Unit = {
    Await.result(system.terminate(), config.getDuration("glint.default.shutdown-timeout", TimeUnit.MILLISECONDS) milliseconds)
  }

}
