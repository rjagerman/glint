name := "Glint"

version := "0.2"

organization := "ch.ethz.inf.da"

scalaVersion := "2.11.11"

fork in Test := true


// Akka

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.3"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.5.3"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.3"


// Retry

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

libraryDependencies += "me.lessis" %% "retry" % "0.2.0"


// Breeze

libraryDependencies += "org.scalanlp" %% "breeze" % "0.13.2"

libraryDependencies += "org.scalanlp" %% "breeze-natives" % "0.13.2"


// Unit tests

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"


// Performance benchmarking

libraryDependencies += "com.storm-enroute" %% "scalameter" % "0.8.2" % "provided"


// Scala option parser

libraryDependencies += "com.github.scopt" %% "scopt" % "3.6.0"


// Logging

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"


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

