# [ ![Glint](https://github.com/rjagerman/glint/wiki/images/glint-logo-small.png "Glint") ](https://github.com/rjagerman/glint)
Glint is a high performance [Scala](http://www.scala-lang.org/) parameter server, currently in development.
The aim is to make it easy to develop performant distributed machine learning algorithms using the parameter server architecture as a consistency model. One of the major goals is compatibility with Spark.

The code is still under heavy development and is not yet published to any available repository. Once the library is more mature and ready to be used it will be published. To use the current version one should compile the system manually, publish it to a local repository and include it in your project through sbt.

## Compilation
To compile the code, clone this repository and run:

    sbt compile assembly package publish-local

This should publish the library jar file to the local ivy2 repository, which means you can then use it in your project's `build.sbt` (on the same machine) as follows:

    libraryDependencies += "ch.ethz.inf.da" %% "glint" % "0.1-SNAPSHOT"

## Running

The parameter server is designed to be ran stand alone as a separate java process. Future work includes the integration with proper cluster management tools such as Yarn.

The first thing you have to do is configure the parameter server for your cluster. The default configuration is sufficient for running a simple localhost test. An example configuration can be found in [src/main/resources/glint.conf](src/main/resources/glint.conf). Most importantly you would want to set the master's hostname and port. This makes it so every parameter server and client interface knows where to find the master node.

After having created a `.conf` file we can run a master and multiple parameter servers:

    # On your master machine
    java -jar target/scala-2.11/Glint-assembly-0.1-SNAPSHOT.jar master -c your-conf-file.conf

    # On your worker machines (e.g. worker1 and worker2)
    java -jar target/scala-2.11/Glint-assembly-0.1-SNAPSHOT.jar server -h localhost -c your-conf-file.conf
    java -jar target/scala-2.11/Glint-assembly-0.1-SNAPSHOT.jar server -h localhost -c your-conf-file.conf

The `-c your-conf-file.conf` is optional and you can omit it to use the default settings when just running a test on localhost. 

## Example usage

With your parameter server up and running you can use the library to connect to the parameter servers, construct distributed models and pull/push data.

**!! As the software is currently under heavy development, this will change considerably over time.**

The parameter server is designed to be highly asynchronous. The code uses common concurrency methods from scala. If you are unfamiliar with the concepts of Futures, ExecutionContext, etc. it is best to read up on those first.

The first thing you want to do is construct a Client that is connected to the master:

    import scala.concurrent.{Await, ExecutionContext}
    import scala.concurrent.duration._
    import glint.Client
    implicit val ec = ExecutionContext.Implicits.global
    val client = Await.result(Client(ConfigFactory.parseFile(new java.io.File("<config-file>.conf"))), 30 seconds)

Now we can use this client object to construct a large model distributed over the parameter servers. The example below constructs a large scalar model storing integers with 100000 keys (indexed 0...99999) and default value 0.

    val model = Await.result(gc.denseScalarModel[Int]("counttable", 100000, 0), 30 seconds)
    
Next, this model can be used to pull/push data to and from the parameter server:

    // Pull key (index) "3" from the parameter servers
    model.pullSingle(3).onSuccess { case value => println(value) }
    
    // Push new data to index "3"
    // The parameter server aggregates pushes by adding it to the old value.
    // In this case we are adding "5" to the old value of index/key 3
    model.pushSingle(3, 5)

Note that both of the above operations happen asynchronously and return a `Future[...]` immediately. You can manually wait for this future by using `Await.result(...)` or you can provide a callback such as `onSuccess { }`.

## Next steps

Current development goals:

1. Improve partitioning schemes
2. Use the library in our LDA code
3. Make it easy to batch push/pull requests together
4. Optimize, optimize and then optimize some more
5. Finish thesis ;-)

Future development goals:

1. Support for proper cluster setup (e.g. Yarn)
2. Generalize push update rule to allow for more than just addition
