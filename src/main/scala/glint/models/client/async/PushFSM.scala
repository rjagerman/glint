package glint.models.client.async

import akka.pattern.{AskTimeoutException, ask}
import akka.actor.ActorRef
import akka.util.Timeout
import glint.exceptions.PushFailedException
import glint.messages.server.logic._

import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration._

/**
  * A push-mechanism using a finite state machine to guarantee exactly-once delivery with multiple attempts
  *
  * @param message A function that takes an identifier and generates a message of type T
  * @param actorRef The actor to send to
  * @param maximumAttempts The maximum number of attempts
  * @param maximumLogicAttempts The maximum number of attempts to establish communication through logic channels
  * @param initialTimeout The initial timeout for the request
  * @param backoff The backoff multiplier
  * @param backoffCeiling The limit on the backoff timeout
  * @param ec The execution context
  * @tparam T The type of message to send
  */
class PushFSM[T](message: Int => T,
                 actorRef: ActorRef,
                 maximumAttempts: Int = 10,
                 maximumLogicAttempts: Int = 100,
                 initialTimeout: FiniteDuration = 1 second,
                 backoff: Double = 1.6,
                 backoffCeiling: FiniteDuration = 10 minutes)(implicit ec: ExecutionContext) {

  private implicit var timeout: Timeout = new Timeout(initialTimeout)

  // Unique identifier for this push
  private var id = 0

  // Internal counter for the number of push attempts
  private var attempts = 0
  private var logicAttempts = 0

  // Check if this request has already ran
  private var ran = false

  // The promise to eventually complete
  private val promise: Promise[Boolean] = Promise[Boolean]()

  /**
    * Runs the push request
    */
  def run(): Future[Boolean] = {
    if (!ran) {
      prepare()
      ran = true
    }
    promise.future
  }

  /**
    * Prepare state
    * Attempts to obtain a unique and available identifier from the parameter server for the next push request
    */
  private def prepare(): Unit = {
    val prepareFuture = actorRef ? GetUniqueID()
    prepareFuture.onSuccess {
      case UniqueID(identifier) =>
        id = identifier
        execute()
      case _ =>
        retry(prepare)
    }
    prepareFuture.onFailure {
      case ex: AskTimeoutException =>
        timeBackoff()
        retry(prepare)
      case _ =>
        retry(prepare)
    }
  }

  /**
    * Execute the push request performing a single push
    */
  private def execute(): Unit = {
    if (attempts >= maximumAttempts) {
      promise.failure(new PushFailedException(s"Failed $attempts out of $maximumAttempts attempts to push data"))
    } else {
      attempts += 1
      actorRef ! message(id)
      acknowledge()
    }
  }

  /**
    * Acknowledge state
    * We keep sending acknowledge messages until we either receive a not acknowledge or acknowledge reply
    */
  private def acknowledge(): Unit = {
    val acknowledgeFuture = actorRef ? AcknowledgeReceipt(id)
    acknowledgeFuture.onSuccess {
      case NotAcknowledgeReceipt(identifier) if identifier == id =>
        execute()
      case AcknowledgeReceipt(identifier) if identifier == id =>
        promise.success(true)
        forget()
      case _ =>
        retry(acknowledge)
    }
    acknowledgeFuture.onFailure {
      case ex: AskTimeoutException =>
        timeBackoff()
        retry(acknowledge)
      case _ =>
        retry(acknowledge)
    }
  }

  /**
    * Forget state
    * We keep sending forget messages until we receive a successfull reply
    */
  private def forget(): Unit = {
    val forgetFuture = actorRef ? Forget(id)
    forgetFuture.onSuccess {
      case Forget(identifier) if identifier == id =>
        ()
      case _ =>
        forget()
    }
    forgetFuture.onFailure {
      case ex: AskTimeoutException =>
        timeBackoff()
        forget()
      case _ =>
        forget()
    }
  }

  /**
    * Increase the timeout with an exponential backoff
    */
  private def timeBackoff(): Unit = {
    if (timeout.duration.toMillis * backoff > backoffCeiling.toMillis) {
      timeout = new Timeout(backoffCeiling)
    } else {
      timeout = new Timeout((timeout.duration.toMillis * backoff) millis)
    }
  }

  /**
    * Retries a function while keeping track of a logic attempts counter and fails when the logic attempt counter is
    * too large
    *
    * @param func The function to execute again
    */
  private def retry(func: () => Unit): Unit = {
    logicAttempts += 1
    if (logicAttempts < maximumLogicAttempts) {
      func()
    } else {
      promise.failure(new PushFailedException(s"Failed $logicAttempts out of $maximumLogicAttempts attempts to communicate"))
    }
  }

}
