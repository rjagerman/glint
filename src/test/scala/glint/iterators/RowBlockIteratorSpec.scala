package glint.iterators

import akka.util.Timeout
import glint.SystemTest
import glint.mocking.MockBigMatrix
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import breeze.linalg.Vector

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * RetryBigMatrix test specification
  */
class RowBlockIteratorSpec extends FlatSpec with SystemTest with Matchers with PrivateMethodTester {

  "A RowBlockIterator" should "iterate over all blocks of rows in order" in {

    // Construct mock matrix and data to push into it
    val nrOfRows = 5
    val nrOfCols = 2
    val mockMatrix = new MockBigMatrix[Long](nrOfRows, nrOfCols, 0, _ + _)

    val rows   = Array(0L, 0L, 1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L)
    val cols   = Array( 0,  1,  0,  1,  0,  1,  0,  1,  0,  1)
    val values = Array(0L,  1,  2,  3,  4,  5,  6,  7,  8,  9)

    whenReady(mockMatrix.push(rows, cols, values)) { identity }

    // Check whether elements are in order
    var counter = 0
    val iterator = new RowBlockIterator[Long](mockMatrix, 2)
    iterator.foreach {
      case rows => rows.foreach {
        case row => row.foreach {
          case value =>
            assert(value == counter)
            counter += 1
        }
      }
    }

  }

  it should "iterate over a single block" in {

    // Construct mock matrix and data to push into it
    val nrOfRows = 5
    val nrOfCols = 2
    val mockMatrix = new MockBigMatrix[Long](nrOfRows, nrOfCols, 0, _ + _)

    val rows = Array(0L, 0L, 1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L)
    val cols = Array(0, 1, 0, 1, 0, 1, 0, 1, 0, 1)
    val values = Array(0L, 1, 2, 3, 4, 5, 6, 7, 8, 9)

    whenReady(mockMatrix.push(rows, cols, values)) {
      identity
    }

    // Check whether elements are in order
    var counter = 0
    val iterator = new RowBlockIterator[Long](mockMatrix, 7)
    val resultRows = iterator.next()
    assert(!iterator.hasNext)
    resultRows.foreach {
      case row => row.foreach {
        case value =>
          assert(value == counter)
          counter += 1
      }
    }

  }

  it should "not iterate over an empty matrix" in {
    val mockMatrix = new MockBigMatrix[Double](0, 5, 0, _ + _)

    val iterator = new RowBlockIterator[Double](mockMatrix, 3)
    assert(!iterator.hasNext)
    iterator.foreach {
      case _ => fail("This should never execute")
    }

  }

  it should "fetchNextFuture properly" in {

    // Construct mock matrix and data to push into it
    val nrOfRows = 5
    val nrOfCols = 2
    val mockMatrix = new MockBigMatrix[Long](nrOfRows, nrOfCols, 0, _ + _)

    val rows   = Array(0L, 0L, 1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L)
    val cols   = Array( 0,  1,  0,  1,  0,  1,  0,  1,  0,  1)
    val values = Array(0L,  1,  2,  3,  4,  5,  6,  7,  8,  9)

    whenReady(mockMatrix.push(rows, cols, values)) { identity }

    val iterator = new RowBlockIterator[Long](mockMatrix, 2)

    val fetchNextFuture = PrivateMethod[RowBlockIterator[Long]]('fetchNextFuture)

    (iterator invokePrivate fetchNextFuture()).asInstanceOf[Future[Array[Vector[Long]]]].futureValue shouldEqual Array(Vector(0L, 1), Vector(2L, 3))
    iterator.next()
    (iterator invokePrivate fetchNextFuture()).asInstanceOf[Future[Array[Vector[Long]]]].futureValue shouldEqual Array(Vector(4L, 5), Vector(6L, 7))
    iterator.next()
    (iterator invokePrivate fetchNextFuture()).asInstanceOf[Future[Array[Vector[Long]]]].futureValue shouldEqual Array(Vector(8L, 9))

  }

}
