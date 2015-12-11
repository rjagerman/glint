# [![Glint](https://github.com/rjagerman/glint/wiki/images/glint-logo-small.png "Glint")](https://github.com/rjagerman/glint)
[![Build Status](https://travis-ci.org/rjagerman/glint.svg?branch=add-ci-testing)](https://travis-ci.org/rjagerman/glint)

Glint is a high performance [Scala](http://www.scala-lang.org/) parameter server built using [Akka](http://akka.io/).
The aim is to make it easy to develop performant distributed machine learning algorithms using the parameter server architecture as a consistency model. One of the major goals is compatibility with [Spark](http://spark.apache.org/).

## Compile
To use the current version you should compile the system manually, publish it to a local repository and include it in your project through sbt. Clone this repository and run:

    sbt "+ compile" "+ assembly" "+ publish-local"

The `+` indicates that it should compile for all versions defined in the `build.sbt` file. The command will compile, assemble and publish the library jar file to the local ivy2 repository, which means you can then use it in your project's `build.sbt` (on the same machine) as follows:

    libraryDependencies += "ch.ethz.inf.da" %% "glint" % "0.1-SNAPSHOT"

## Starting parameter servers

The parameter server is designed to be ran stand alone as a separate java process.

The first thing you have to do is configure the parameter server for your cluster. The default configuration is sufficient for running a simple localhost test. An example configuration can be found in [src/main/resources/glint.conf](src/main/resources/glint.conf). Most importantly you would want to set the master's hostname and port. This makes it so every parameter server and client interface knows where to find the master node.

After having created a `.conf` file we can run a master and multiple parameter servers:

    # On your master machine
    java -jar target/scala-2.11/Glint-assembly-0.1-SNAPSHOT.jar master -c your-conf-file.conf

    # On your worker machines (e.g. worker1 and worker2)
    java -jar target/scala-2.11/Glint-assembly-0.1-SNAPSHOT.jar server -h localhost -c your-conf-file.conf
    java -jar target/scala-2.11/Glint-assembly-0.1-SNAPSHOT.jar server -h localhost -c your-conf-file.conf

The `-c your-conf-file.conf` is optional and you can omit it to use the default settings when just running a test on localhost.

Alternatively you can use the provided scripts in the `sbin` folder in combination with several settings in the `conf` folder to automatically start a stand-alone cluster of parameter server on your machines. These scripts require passwordless ssh access to the machines on which you wish to start the parameter servers.

## Example usage

With your parameter server up and running you can use the library to connect to the parameter servers, construct distributed matrices/vectors and pull/push data.

The parameter server is designed to be highly asynchronous. The code uses common concurrency methods from scala. If you are unfamiliar with the concepts of Futures, ExecutionContext, etc. it is best to read up on those first.

The first thing you want to do is construct a Client that is connected to the master:

    import scala.concurrent.{Await, ExecutionContext}
    import scala.concurrent.duration._
    import glint.Client
    implicit val ec = ExecutionContext.Implicits.global
    val client = Await.result(Client(ConfigFactory.parseFile(new java.io.File("<config-file>.conf"))), 30 seconds)

Now we can use this client object to construct a large model distributed over the parameter servers. The example below constructs a large matrix storing integers with 10000 rows and 10000 columns:

    val model = Await.result(gc.matrix[Int](10000, 10), 30 seconds)
    
Next, this model can be used to pull/push data to and from the parameter server:

    // Push new data to row 3 and column 5
    // The parameter server aggregates pushes by adding it to the old value.
    // In this case we are adding "109" to the old value of (3,5)
    model.push(Array(999L), Array(5), Array(5))

    // Pull row 999 from the parameter server, this will return the entire row (all 10 values)
    model.pull(Array(999L)).onSuccess { case values => println(values(0)) }

Note that both of the above operations happen asynchronously and return a `Future[...]` immediately. You can manually wait for this future by using `Await.result(...)` or you can provide a callback such as `onSuccess { }`.

