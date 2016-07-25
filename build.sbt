name := "Glint"

version := "0.1-SNAPSHOT"

organization := "ch.ethz.inf.da"

scalaVersion := "2.10.6"

crossScalaVersions := Seq("2.10.6", "2.11.8")

fork in Test := true

// Spark

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.0" % "provided"

// Akka

val akkaVersion = scalaVersion {
  case x if x.startsWith("2.11") && System.getProperty("java.version") > "1.8" => "2.4.7"
  case _ => "2.3.15"
}

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion.value

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion.value

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion.value

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

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Performance benchmarking

libraryDependencies += "com.storm-enroute" %% "scalameter" % "0.7" % "provided"

// Scala option parser

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

// Logging

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"

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

