# Spark integration

So far you have seen a very short introduction to programming with the Glint parameter server. At its core you will
construct a client that will construct either matrices or vectors. You then use the `pull` and `push` methods on these
matrices or vectors to query and update the current state of the matrix. Now how does this relate to spark?

The main idea is that the `BigMatrix` and `BigVector` objects that you obtain from the client are serializable and 
are safe to be used within Spark closures. It should be noted that operations such as `push` and `pull` on the 
BigMatrix and BigVector objects will need access to the implicit execution context and the implicit timeout. These 
objects are not serializable which can cause problems when running a closure naively.

## Example: add values of an RDD to a distributed vector
In this example we will have an RDD that contains tuples of (Int, Double). The Int represents the key (or index) of a 
vector and the Double represents the corresponding value. We want to add these values to a distributed vector that is 
stored on the parameter servers. First, let's open the spark-shell and add the Glint jar dependency:

    $ spark-shell --jars ./target/scala-2.10/Glint-assembly-0.1-SNAPSHOT.jar
    
Meanwhile, make sure the parameter servers are up and running in separate terminal windows:
 
    $ sbt "run master"
    $ sbt "run server"
    $ sbt "run server"
 
Let's construct a client (make sure to mark it as transient so the spark-shell doesn't try to serialize it):

    import glint.Client
    @transient val client = Client()
    
Now, let's create our data of (key, value) pairs as an RDD:
    
    val rdd = sc.parallelize(Array((0, 0.0), (1, 2.0), (2, -9.0), (3, 5.5), (4, 3.14), (5, 55.5), (6, 0.01), (7, 10.0), (8, 100.0), (9, 1000.0)))

And a distributed vector:

    val vector = client.vector[Double](10)
    
The main code will use the constructed `vector` object within a spark closure such as `rdd.foreach { ... }`. There is 
some additional boilerplate to deal with the execution context and timeouts. This is, however, a small price to pay 
for the gained flexibility and customizability of the concurrency of your code.

    import scala.concurrent.ExecutionContext
    rdd.foreach {
        case (index, value) =>
            implicit val ec = ExecutionContext.Implicits.global
            vector.push(Array(index), Array(value))
    }
    
Finally, let's verify the result by pulling the values from the parameter server:

    @transient implicit val ec = ExecutionContext.Implicits.global
    vector.pull((0L until 10).toArray).onSuccess {
        case values => println(values.mkString(", "))
    }

We should observe the expected output:

    0.0, 2.0, -9.0, 5.5, 3.14, 55.5, 0.01, 10.0, 100.0, 1000.0
