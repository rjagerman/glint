# Pushing values

In this section of the guide we will push some values from the parameter server. Make sure your `sbt console` is still open and the matrix from the previous pull section is still available

Now let's push some values to the matrix

    val result = matrix.push(Array(0L, 1L, 2L), Array(100, 200, 300), Array(0.1, 3.1415, 9.999))

This will push the following values:

 * Add 0.1 to the current value of row 0, column 100
 * Add 3.1415 to the current value of row 1, column 200
 * Add 9.999 to the current value row 2, column 300

This method is asynchronous and it returns a `Future[Boolean]` immediately. Attach a callback to verify that the process completed

    result.onSuccess {
        case true => println("Push successfull")
    }

Now let's check whether these new values are indeed on the parameter server by pulling the values:

    matrix.pull(Array(0L, 1L, 2L), Array(100, 200, 300)).onSuccess {
        case values => println(values.mkString(", "))
    }

This should indeed print out the values:

    0.1, 3.1415, 9.999

Now we will show the additive nature of updates of the parameter server. Let's add 0.1 to row 0, column 100 to turn the current value of `0.1` into `0.2`. Let's wait for that update to complete by using Scala's `Await` concurrency method to block execution:

    import scala.concurrent.Await
    Await.result(matrix.push(Array(0L), Array(100), Array(0.1)), 30 seconds)

Now verify the result on the parameter server:

    matrix.pull(Array(0L), Array(100)).onSuccess {
        case values => println(values.mkString(", "))
    }

And we should observe the expected result:

    0.2

[Continue to Spark Integration](spark.md)