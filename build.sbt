name := "blob-to-kafka"
organization := "Aquity"
version := "1.0"

// naviblob is not support on 2.13.4
scalaVersion := "2.13.4"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
javaOptions ++= Seq("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-Xmx2G")
val akkaVersion = "2.6.13"

libraryDependencies ++= Seq(

  "com.sksamuel.avro4s" %% "avro4s-core" % "4.0.4",
  "tech.navicore" %% "navipath" % "4.0.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe" % "config" % "1.4.1",
  "com.github.pureconfig" %% "pureconfig" % "0.14.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "joda-time" % "joda-time" % "2.10.6",

  "com.azure" % "azure-storage-blob" % "12.7.0",

  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.7",

  "org.scalatest" %% "scalatest-flatspec" % "3.2.6" % Test
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) =>
    xs map(_.toLowerCase) match {
      case "manifest.mf" :: Nil =>
        MergeStrategy.discard
      case ps@_ :: _ if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

// do not run tests when building the assembly
test in assembly := {}
// specify the main class and jar name
mainClass in assembly := Some("bdtlab.replay.Main")
assemblyJarName in assembly := "app.jar"

coverageMinimum := 90
coverageFailOnMinimum := true
coverageHighlighting := true
publishArtifact in Test := false
coverageExcludedPackages := ".*Main.*;.*BlobClient.*"


