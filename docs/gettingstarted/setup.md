# Project setup

Before we start we need to setup a new scala project and add the correct dependencies. In this example we will use the Scala build tool (sbt) to do this.

## Creating an SBT project

Let's first create a directory for the project and initialize sbt there.

    mkdir glint-tutorial
    cd glint-tutorial
    sbt initialize

Add a `build.sbt` file with the following contents:

    name := "GlintTutorial"
    
    version := "0.1-SNAPSHOT"
    
    scalaVersion := "2.10.6"
    
    libraryDependencies += "ch.ethz.inf.da" %% "glint" % "0.1-SNAPSHOT"

## Adding Glint dependency

The listed glint dependency is not publicly available on a central repository yet since the software is still in alpha. This will change in future versions. For now you will have to compile glint manually and publish its package locally.

Grab the latest version of glint from the github repository:
    
    cd ../
    git clone git@github.com:rjagerman/glint.git
    cd glint
    sbt compile assembly publish-local

This will compile a version of Glint and publish the binaries to a local ivy2 repository. After this step, the sbt dependency should work.

## Opening a console

Now, run `sbt console` to open an interactive console with sbt:

    $ sbt console
    scala> 

The next steps in the guide will be executed from this console.

[Continue to Constructing Models](constructing.md)
