package glint.iterators

import akka.util.Timeout
import glint.SystemTest
import glint.mocking.MockBigMatrix
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * RetryBigMatrix test specification
  */
class ColumnIteratorSpec extends FlatSpec with SystemTest with Matchers {

  "A ColumnIterator" should "iterate over all columns in order" in {

    // Construct mock matrix and data to push into it
    val nrOfRows = 2
    val nrOfCols = 4
    val mockMatrix = new MockBigMatrix[Long](nrOfRows, nrOfCols, 0, _ + _)

    val rows   = Array(0L, 1L, 0L, 1L, 0L, 1L, 0L, 1L)
    val cols   = Array( 0,  0,  1,  1,  2,  2,  3,  3)
    val values = Array(0L,  1,  2,  3,  4,  5,  6,  7)

    whenReady(mockMatrix.push(rows, cols, values)) { identity }

    // Check whether elements are in order
    var counter = 0
    val iterator = new ColumnIterator[Long](mockMatrix)
    iterator.foreach {
      case column => column.foreach {
        case value =>
          assert(value == counter)
          counter += 1
      }
    }

  }

  it should "iterate over all columns in order with larger rows" in {

    // Construct mock matrix and data to push into it
    val nrOfRows = 4
    val nrOfCols = 2
    val mockMatrix = new MockBigMatrix[Long](nrOfRows, nrOfCols, 0, _ + _)

    val rows   = Array(0L, 1L, 2L, 3L, 0L, 1L, 2L, 3L)
    val cols   = Array( 0,  0,  0,  0,  1,  1,  1,  1)
    val values = Array(0L,  1,  2,  3,  4,  5,  6,  7)

    whenReady(mockMatrix.push(rows, cols, values)) { identity }

    // Check whether elements are in order
    var counter = 0
    val iterator = new ColumnIterator[Long](mockMatrix)
    iterator.foreach {
      case column => column.foreach {
        case value =>
          assert(value == counter)
          counter += 1
      }
    }

  }

  it should "not iterate over an empty matrix" in {
    val mockMatrix = new MockBigMatrix[Double](0, 2, 0, _ + _)

    val iterator = new ColumnIterator[Double](mockMatrix)
    assert(!iterator.hasNext)
    iterator.foreach {
      case _ => fail("This should never execute")
    }

  }

}
