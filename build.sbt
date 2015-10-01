name := "Glint"

version := "0.1"

scalaVersion := "2.10.5"


// Akka

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT"

// Breeze native BLAS support

libraryDependencies += "org.scalanlp" %% "breeze" % "0.11.2"

libraryDependencies += "org.scalanlp" %% "breeze-natives" % "0.11.2"

// Apache Commons IO

libraryDependencies += "commons-io" % "commons-io" % "2.4"

// Unit tests

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"


// Resolvers

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

resolvers += Resolver.sonatypeRepo("public")

resolvers += Resolver.sonatypeRepo("snapshots")

