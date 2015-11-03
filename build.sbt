name := "Glint"

version := "0.1-SNAPSHOT"

organization := "ch.ethz.inf.da"

scalaVersion := "2.10.6"

crossScalaVersions := Seq("2.10.6", "2.11.7")

// Akka

libraryDependencies <+= scalaVersion {
  case "2.10.6" => "com.typesafe.akka" %% "akka-actor" % "2.3.14"
  case _ => "com.typesafe.akka" %% "akka-actor" % "2.4.0"
}

libraryDependencies <+= scalaVersion {
  case "2.10.6" => "com.typesafe.akka" %% "akka-remote" % "2.3.14"
  case _ => "com.typesafe.akka" %% "akka-remote" % "2.4.0"
}

//libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.4.0"

// Breeze native BLAS support

libraryDependencies += "org.scalanlp" %% "breeze" % "0.11.2"

libraryDependencies += "org.scalanlp" %% "breeze-natives" % "0.11.2"

// Unit tests

libraryDependencies <+= scalaVersion {
  case "2.10.6" => "org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"
  case _ => "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
}

// Scala option parser

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

// Logging

libraryDependencies <+= scalaVersion {
  case "2.10.6" =>  "com.typesafe.scala-logging" % "scala-logging-slf4j_2.10" % "2.1.2"
  case _ => "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
}

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.3"

// Resolvers

resolvers += Resolver.sonatypeRepo("public")