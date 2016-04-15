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

It should be noted that operations such as `push` and `pull` on the BigMatrix and BigVector objects will need access to the implicit execution context and the implicit timeout. These objects are not serializable which can cause problems when running a closure naively. In the above example you will typically want to do something like this:

    rdd.foreachPartition {
        case values => 
          implicit val ec = ExecutionContext.Implicits.global
          implicit val timeout = new Timeout(30 seconds)

          values.foreach {
            case value => vector.push(Array(value), Array(12))
          }

    }

If your execution context and timeout are defined outside the scope of the closure, Spark will attempt to serialize them and throw an error. This also means that running within the Spark shell is currently not possible due to the shell's attempts to serialize everything. We hope to resolve this in future versions to make it easier to prototype with.