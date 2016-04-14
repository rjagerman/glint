# Constructing models

In order to construct models on the parameter servers we first need to start a master node and at least one parameter server. Refer to the [localhost deployment guide](../deploymentguide/localhost/) how to start these nodes on your localhost. Make sure you have a master node and one or more server nodes up and running in separate terminal windows before continuing.

## Connect to master

Before we can spawn distributed models on the parameter server, we first need to connect to the running master node. We assume that this is running on localhost. Let's create a configuration file for our project first. Call it `glint.conf` and place it in the project's root directory. Give it the following contents:

    glint.master.host = "127.0.0.1"
    glint.master.port = 13370
    glint.client.timeout = 30s

Open the sbt console if you had closed it, and enter the following commands:

    import com.typesafe.config.ConfigFactory
    import glint.Client

    val client = Client(ConfigFactory.parseFile(new java.io.File("glint.conf")))

This will construct a client object that acts as an interface to the parameter servers. We can now use this client object to construct distributed matrices and vectors on the parameter server.

## Construct a matrix

Now that the Client object is connected to the Master node we can try to construct a distributed matrix. Let's create a distributed matrix that stores `Double` values and has 10000 rows with 2000 columns:

    val matrix = client.matrix[Double](10000, 2000)

Before we can interact with this matrix we need to define an execution context and timeout for the requests. Here we use the default global execution context and a timeout of 30 seconds:

    import scala.concurrent.duration._
    import akka.util.Timeout
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
    implicit val timeout = new Timeout(30 seconds)

To destroy the matrix and release the allocated resources on the parameter servers, run:

    val result = matrix.destroy()

This method is asynchronous and returns a `Future[Boolean]` object immediately. This future indicates the eventual success or failure of the operation. Let's attach a callback to verify if the operation succeeded:

    result.onSuccess {
        case true => println("Succesfully destroyed the matrix")
    }

## Construct a vector

A vector can be constructed almost analogously. For example a vector that stores `Long` values with 100,000 dimensions, would look like this:

    val vector = client.vector[Long](100000)

And indeed, we can destroy this vector in a similar way:

    val result = vector.destroy()
    result.onSuccess {
        case true => println("Succesfully destroyed the vector")
    }

[Continue to Pulling values](pull.md)
