# Localhost Deployment

To run the system on a single computer is quite simple. The default configuration uses localhost and thus no modifications are necessary.

Make sure you have a recent version of glint from github and that you have the [Scala build tool](http://www.scala-sbt.org/) installed

## Run a master node

To run a single master node (by default on port 13370), use sbt:

    sbt "run master"

## Run a parameter server

Open a new terminal window and start a parameter server by running

    sbt "run server"

You can repeat this multiple times to start multiple parameter servers.

## Specifying custom configuration

Sometimes you will want to specify different configuration. For example, when you want to send very large messages and increase Akka's message size limit or if you want to use a different port number. You can create a configuration file and load it as follows:

    sbt "run master -c /path/to/configuration/file.conf"
    sbt "run server -c /path/to/configuration/file.conf"

For most scenarios where you want to run a localhost version of the parameter server this is not necessary and the default configuration will suffice.