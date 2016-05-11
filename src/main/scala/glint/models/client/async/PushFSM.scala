package glint.models.client.async

import java.util.concurrent.TimeUnit

import akka.pattern.{AskTimeoutException, ask}
import akka.actor.ActorRef
import akka.util.Timeout
import com.typesafe.config.Config
import glint.exceptions.PushFailedException
import glint.messages.server.logic._

import scala.concurrent.{Future, ExecutionContext, Promise}
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * A push-mechanism using a finite state machine to guarantee exactly-once delivery with multiple attempts
  *
  * @param message A function that takes an identifier and generates a message of type T
  * @param actorRef The actor to send to
  * @param maximumAttempts The maximum number of attempts
  * @param maximumLogicAttempts The maximum number of attempts to establish communication through logic channels
  * @param initialTimeout The initial timeout for the request
  * @param maximumTimeout The maximum timeout for the request
  * @param backoffMultiplier The backoff multiplier
  * @param ec The execution context
  * @tparam T The type of message to send
  */
class PushFSM[T](message: Int => T,
                 actorRef: ActorRef,
                 maximumAttempts: Int,
                 maximumLogicAttempts: Int,
                 initialTimeout: FiniteDuration,
                 maximumTimeout: FiniteDuration,
                 backoffMultiplier: Double)(implicit ec: ExecutionContext) {

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
    if (timeout.duration.toMillis * backoffMultiplier > maximumTimeout.toMillis) {
      timeout = new Timeout(maximumTimeout)
    } else {
      timeout = new Timeout((timeout.duration.toMillis * backoffMultiplier) millis)
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


object PushFSM {

  private var maximumAttempts: Int = 10
  private var maximumLogicAttempts: Int = 100
  private var initialTimeout: FiniteDuration = 5 seconds
  private var maximumTimeout: FiniteDuration = 5 minutes
  private var backoffMultiplier: Double = 1.6

  /**
    * Initializes the FSM default parameters with those specified in given config
    *
    * @param config The configuration to use
    */
  def initialize(config: Config): Unit = {
    maximumAttempts = config.getInt("glint.push.maximum-attempts")
    maximumLogicAttempts = config.getInt("glint.push.maximum-logic-attempts")
    initialTimeout = new FiniteDuration(config.getDuration("glint.push.initial-timeout", TimeUnit.MILLISECONDS),
      TimeUnit.MILLISECONDS)
    maximumTimeout = new FiniteDuration(config.getDuration("glint.push.maximum-timeout", TimeUnit.MILLISECONDS),
      TimeUnit.MILLISECONDS)
    backoffMultiplier = config.getInt("glint.push.backoff-multiplier")
  }

  /**
    * Constructs a new FSM for given message and actor
    *
    * @param message A function that takes an identifier and generates a message of type T
    * @param actorRef The actor to send to
    * @param ec The execution context
    * @tparam T The type of message to send
    * @return An new and initialized PushFSM
    */
  def apply[T](message: Int => T, actorRef: ActorRef)(implicit ec: ExecutionContext): PushFSM[T] = {
    new PushFSM[T](message, actorRef, maximumAttempts, maximumLogicAttempts, initialTimeout, maximumTimeout,
      backoffMultiplier)
  }

}

