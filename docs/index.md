# ![Glint](https://raw.githubusercontent.com/wiki/rjagerman/glint/images/glint-logo-small-notext.png) Glint

Glint is a high-performance parameter server compatible with Spark. It provides a high-level API in Scala making it easy for users to build machine learning algorithms with the parameter server as a concurrency model.

## Parameter Server

A parameter server is a specialized key-value store for mathematical data structures such as matrices and vectors. It has been succesfully applied in both industry and academia as a tool for distributing the model space of machine learning algorithms to many machines.

![Overview](https://raw.githubusercontent.com/wiki/rjagerman/glint/images/overview.png)

## Download

The software is currently still in alpha. Refer to the compile section in order to compile and build the software manually. As we move to beta this page will provide versioned executables ready to download.

## Compile

Glint uses [sbt](http://www.scala-sbt.org/) as a package manager. To compile, run the following commands from a command-line

    git clone git@github.com:rjagerman/glint.git
    cd glint
    sbt compile assembly

This will (by default) compile for Scala version 2.10. If you wish to compile binaries for both 2.10 and 2.11, use:

    sbt "+ compile" "+ assembly"

A binary jar file is produced in `target/scala-2.10/Glint-assembly-0.1-SNAPSHOT.jar`.

## Run

To run the parameter server on your localhost, you have two options: Either compile and run the .jar file or use `sbt run`. To start a master node, use one of the following commands:

  * `java -jar target/scala-2.10/Glint-assembly-0.1-SNAPSHOT.jar master`
  * `sbt "run master"` 

To start a parameter server node, use one of the following commands:

  * `java -jar target/scala-2.10/Glint-assembly-0.1-SNAPSHOT.jar server`
  * `sbt "run server"`

## Where to go next?

* [Getting Started](gettingstarted/index.md): Shows how to use the API and program applications with Glint
* [Deployment Guide](deploymentguide/index.md): Shows how to deploy Glint in a stand-alone way on a cluster
* [Examples](examples/index.md): Lists several example applications that use Glint

