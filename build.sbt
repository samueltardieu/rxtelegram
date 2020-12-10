import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

lazy val rxTelegram = project
  .in(file("."))
  .settings(
    name := "rxtelegram",
    organization := "net.rfc1149",
    version := "0.0.2-SNAPSHOT",
    scalaVersion := "2.13.4",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.6.10",
      "com.typesafe.akka" %% "akka-stream" % "2.6.10",
      "com.typesafe.akka" %% "akka-http-core" % "10.1.13",
      "de.heikoseeberger" %% "akka-http-play-json" % "1.35.2",
      "com.iheart" %% "ficus" % "1.5.0",
      "commons-io" % "commons-io" % "2.8.0",
      "org.specs2" %% "specs2-core" % "4.10.5" % "test"
    ),
    fork in Test := true,
    scalariformAutoformat := true,
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignArguments, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(SpacesWithinPatternBinders, false)
      .setPreference(SpacesAroundMultiImports, false))
