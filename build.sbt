name := "Glint"

version := "0.1"

scalaVersion := "2.11.7"


// Akka

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.0"

// Breeze native BLAS support

libraryDependencies += "org.scalanlp" %% "breeze" % "0.11.2"

libraryDependencies += "org.scalanlp" %% "breeze-natives" % "0.11.2"

// Unit tests

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

