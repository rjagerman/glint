package glint.iterators

import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * An iterator that attempts to prefetch next futures through a pipelined design
  *
  * @param ec The implicit execution context in which to execute requests
  * @param timeout The timeout
  */
abstract class PipelineIterator[T]()(implicit ec: ExecutionContext, timeout: Timeout) extends Iterator[T] {

  protected def fetchNextFuture(): Future[T]

  protected var index: Int = 0
  protected var total: Int = 0
  private var nextFuture: Future[T] = fetchNextFuture()

  override def hasNext: Boolean = index < total

  override def next() = {
    val result = Await.result(nextFuture, timeout.duration)
    index += 1
    if (hasNext) {
      nextFuture = fetchNextFuture()
    }
    result
  }

}
