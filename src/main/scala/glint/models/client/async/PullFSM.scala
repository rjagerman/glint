package glint.models.client.async

import akka.pattern.{AskTimeoutException, ask}
import akka.actor.ActorRef
import akka.util.Timeout
import glint.exceptions.PullFailedException

import scala.concurrent.{Promise, Future, ExecutionContext}
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * A pull-mechanism using a finite state machine to perform multiple retries with exponential timeout backoff
  *
  * @param message The pull message to send
  * @param actorRef The actor to send to
  * @param maximumAttempts The maximum number of attempts
  * @param initialTimeout The initial timeout for the request
  * @param backoff The backoff multiplier
  * @param ec The execution context
  * @tparam T The type of message to send
  */
class PullFSM[T, R: ClassTag](message: T,
                              actorRef: ActorRef,
                              maximumAttempts: Int = 10,
                              initialTimeout: FiniteDuration = 15 seconds,
                              backoff: Double = 1.6)(implicit ec: ExecutionContext) {

  // The timeout for the requests
  private implicit var timeout: Timeout = new Timeout(initialTimeout)

  // Keeps track of the number of attempts
  private var attempts = 0

  // Check if this request has already ran
  private var ran = false

  // The promise to eventually complete
  private val promise: Promise[R] = Promise[R]()

  /**
    * Starts running the pull request
    *
    * @return The future containing the eventual value
    */
  def run(): Future[R] = {
    if (!ran) {
      execute()
      ran = true
    }
    promise.future
  }

  /**
    * Execute the request
    */
  private def execute(): Unit = {
    if (attempts < maximumAttempts) {
      attempts += 1
      request()
    } else {
      promise.failure(new PullFailedException(s"Failed $attempts out of $maximumAttempts attempts to push data"))
    }
  }

  /**
    * Performs the actual pull request
    */
  private def request(): Unit = {
    val request = actorRef ? message
    request.onFailure {
      case ex: AskTimeoutException =>
        timeBackoff()
        execute()
      case _ =>
        execute()
    }
    request.onSuccess {
      case response: R =>
        promise.success(response)
      case _ =>
        execute()
    }
  }

  /**
    * Increase the timeout with an exponential backoff
    */
  private def timeBackoff(): Unit = {
    timeout = new Timeout((timeout.duration.toMillis * backoff) millis)
  }

}

