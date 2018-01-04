name := "lab11b"

version := "1.1"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "com.typesafe.akka" %% "akka-testkit_2.12" % "2.5.6",
  "org.scalatest" %% "scalatest_2.12" % "3.0.4" % "test")
