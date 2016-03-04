package glint.iterators

import akka.util.Timeout

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * An iterator that attempts to prefetch next futures through a pipelined design
  *
  * @param duration The duration to wait at most
  * @param ec The implicit execution context in which to execute requests
  */
abstract class PipelineIterator[T](duration: Duration = Duration.Inf)(implicit ec: ExecutionContext) extends Iterator[T] {

  protected def fetchNextFuture(): Future[T]

  protected var index: Int = 0
  protected var total: Int = 0
  private var nextFuture: Future[T] = fetchNextFuture()

  override def hasNext: Boolean = index < total

  override def next() = {
    val result = Await.result(nextFuture, duration)
    index += 1
    if (hasNext) {
      nextFuture = fetchNextFuture()
    }
    result
  }

}
