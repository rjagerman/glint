package glint.models.server

import java.io.{DataOutputStream, PrintWriter}

import akka.actor.ActorRef
import glint.messages.server.logic._

import scala.collection.mutable

/**
  * Encapsulation for common push logic behavior
  */
trait PushLogic {

  /**
    * A set of received message ids
    */
  val receipt: mutable.HashSet[Int] = mutable.HashSet[Int]()

  /**
    * Unique identifier counter
    */
  var uid = 0

  /**
    * Increases the unique id and returns the next unique id
    *
    * @return The next id
    */
  @inline
  private def nextId(): Int = {
    uid += 1
    uid
  }

  /**
    * Handles push message receipt logic
    *
    * @param message The message
    * @param sender The sender
    */
  def handleLogic(message: Any, sender: ActorRef) = message match {
    case GetUniqueID() =>
      sender ! UniqueID(nextId())

    case AcknowledgeReceipt(id) =>
      if (receipt.contains(id)) {
        sender ! AcknowledgeReceipt(id)
      } else {
        sender ! NotAcknowledgeReceipt(id)
      }

    case Forget(id) =>
      if (receipt.contains(id)) {
        receipt.remove(id)
      }
      sender ! Forget(id)
  }

  /**
    * Adds the message id to the received set
    *
    * @param id The message id
    */
  @inline
  def updateFinished(id: Int): Unit = {
    receipt.add(id)
  }

  /**
    * Write Data into fs
    * @param ds DataOutputStream
    * @param op function for really writing
    * @return Boolean if write OK return true, else will be false
    */
  @inline
  def writeToFile(ds: DataOutputStream)(op: PrintWriter => Unit): Boolean = {
    val pw = new PrintWriter(ds)
    try {
      op(pw)
      true
    } catch {
      case err: Throwable =>
        err.printStackTrace()
        false
    } finally {
      pw.close()
    }
  }
}
