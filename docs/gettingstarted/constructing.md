# Constructing models

In order to construct models on the parameter servers we first need to start a master node and at least one parameter server. Refer to the [localhost deployment guide](../deploymentguide/localhost/) how to start these nodes on your localhost. Make sure you have a master node and one or more server nodes up and running in separate terminal windows before continuing.

## Connect to master

Before we can spawn distributed models on the parameter server, we first need to connect to the running master node. 
This is accomplished by constructing a client object that loads a configuration file that contains all the information 
to communicate with the master node and parameter servers. For a localhost test, the default configuration will suffice.
Open the SBT console if you had closed it, and enter the following: 

    import glint.Client    
    val client = Client()
  
This will construct a client object that acts as an interface to the parameter servers. We can now use this client 
object to construct distributed matrices and vectors on the parameter server.

If you need to change the default configuration (for example if your master node is not on localhost but has a 
different hostname or IP), you can create a `glint.conf` file. See [src/main/resources/glint.conf](https://github.com/rjagerman/glint/blob/master/src/main/resources/glint.conf)
for a comprehensive example. To load a custom configuration file, use the following:

    import com.typesafe.config.ConfigFactory
    import glint.Client
    val client = Client(ConfigFactory.parseFile(new java.io.File("/path/to/glint.conf")))

## Construct a matrix

Now that the Client object is connected to the Master node we can try to construct a distributed matrix. Let's create a 
distributed matrix that stores `Double` values and has 10000 rows with 2000 columns:

    val matrix = client.matrix[Double](10000, 2000)

Before we can interact with this matrix we need to define an execution context for the requests. Here we use the 
default global execution context:

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

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
