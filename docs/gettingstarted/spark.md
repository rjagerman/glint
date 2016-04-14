# Spark integration

So far you have seen a very short introduction to programming with the Glint parameter server. At its core you will construct a client that will construct either matrices or vectors. You then use the `pull` and `push` methods on these matrices or vectors to query and update the current state of the matrix. Now how does this relate to spark?

The main idea is that the `BigMatrix` and `BigVector` objects that you obtain from the client are serializable and are safe to be used within Spark closures. So let's give a quick example:

    val rdd = sc.parallelize(Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) // Some spark RDD
    val vector = client.vector[Double](10)

    rdd.foreach {
        case value => vector.push(Array(value), Array(12)) // Add 12 to every index of the vector
    }

    val result = vector.pull((0L until 10).toArray)
    result.onSuccess {
        case values => println(values.mkString(", "))
    }
    result.onComplete {
        case _ => vector.destroy()
    }
