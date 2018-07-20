name := "Glint"

version := "0.1-SNAPSHOT"

organization := "ch.ethz.inf.da"

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.10.6", "2.11.12")

fork in Test := true

// Akka

libraryDependencies <+= scalaVersion {
  case x if x.startsWith("2.11") && System.getProperty("java.version") > "1.8" => "com.typesafe.akka" %% "akka-actor" % "2.5.14"
  case _ => "com.typesafe.akka" %% "akka-actor" % "2.3.15"
}

libraryDependencies <+= scalaVersion {
  case x if x.startsWith("2.11") && System.getProperty("java.version") > "1.8" => "com.typesafe.akka" %% "akka-remote" % "2.5.14"
  case _ => "com.typesafe.akka" %% "akka-remote" % "2.3.15"
}

libraryDependencies <+= scalaVersion {
  case x if x.startsWith("2.11") && System.getProperty("java.version") > "1.8" => "com.typesafe.akka" %% "akka-testkit" % "2.5.14"
  case _ => "com.typesafe.akka" %% "akka-testkit" % "2.3.15"
}


// Retry

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

libraryDependencies += "me.lessis" %% "retry" % "0.2.0"

// Spire (generic fast numerics)

libraryDependencies += "org.spire-math" %% "spire" % "0.7.4"

// Breeze

libraryDependencies += "org.scalanlp" %% "breeze" % "0.11.2"

libraryDependencies += "org.scalanlp" %% "breeze-natives" % "0.11.2"

// Retry

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

libraryDependencies += "me.lessis" %% "retry" % "0.2.0"

// Unit tests

libraryDependencies <+= scalaVersion {
  case x if x.startsWith("2.10") => "org.scalatest" % "scalatest_2.10" % "2.2.4" % "test"
  case x if x.startsWith("2.11") => "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
}

// Performance benchmarking

libraryDependencies += "com.storm-enroute" %% "scalameter" % "0.7" % "provided"

// Scala option parser

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

// Logging

libraryDependencies <+= scalaVersion {
  case "2.10.6" =>  "com.typesafe.scala-logging" % "scala-logging-slf4j_2.10" % "2.1.2"
  case _ => "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
}

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.1" % "provided"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.3" % "provided"

// Hadoop
libraryDependencies += "org.apache.hadoop" % "hadoop-common" % "2.7.2" % "provided"

libraryDependencies += "org.apache.hadoop" % "hadoop-yarn-client" % "2.7.2" % "provided"


// Resolvers

resolvers += Resolver.sonatypeRepo("public")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// Set up scalameter

val scalaMeterFramework = new TestFramework("org.scalameter.ScalaMeterFramework")

testFrameworks += scalaMeterFramework

testOptions in ThisBuild += Tests.Argument(scalaMeterFramework, "-silent")

logBuffered := false

// Testing only sequential (due to binding to network ports)

parallelExecution in Test := false

// Scala documentation
scalacOptions in (Compile, doc) ++= Seq("-doc-root-content", baseDirectory.value+"/docs/root.txt")
scalacOptions in (Compile, doc) ++= Seq("-doc-title", "Glint")
scalacOptions in (Compile, doc) ++= Seq("-skip-packages", "akka")

ghpages.settings

git.remoteRepo := "git@github.com:rjagerman/glint.git"

site.includeScaladoc()
