# Pulling values

In this section of the guide we will pull some values from the parameter server. Make sure your `sbt console` is still open and enter the following command to construct a distributed matrix

    val matrix = client.matrix[Double](10000, 2000)

This will have created a matrix with 10000 rows and 2000 columns that stores `Double` values.


Now in order to pull the current values let's use the pull method:

    val result = matrix.pull(Array(0L, 1L, 2L), Array(100, 200, 300))

This will pull the following values:

 * row 0, column 100
 * row 1, column 200
 * row 2, column 300

Supplying arrays of primitives to the function is done for performance reasons as they have minimal memory overhead. Once again the method is asynchronous and it returns a `Future[Array[Double]]` immediately. This is a placeholder for future values that will eventually (once the request completes) contain the resulting values for the specified rows and columns. Attach an `onSuccess` callback to print these values:

    result.onSuccess {
        case values => println(values.mkString(", "))
    }

This should return three values of exactly 0.0 (which is the default with which the matrix is initialized).

    0.0, 0.0, 0.0

In the following section we will show how to push new values to the parameter server so we can get some more interesting values from the parameter server.

[Continue to Pushing values](push.md)