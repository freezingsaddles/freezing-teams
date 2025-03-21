name           := "Freezing teams"
normalizedName := "freezing-teams"

description := "Team assignment tool."

version := "0.3"

organization         := "org.freezingsaddles"
organizationName     := "Freezing Saddles"
organizationHomepage := Some(url("https://freezingsaddles.org"))

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

scalaVersion := "3.6.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-encoding",
  "UTF-8",
)

libraryDependencies ++= Seq(
  "com.learningobjects" %% "scaloi"     % "0.3.1",
  "com.nrinaudo"        %% "kantan.csv" % "0.8.0",
).map(_.cross(CrossVersion.for3Use2_13))

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt"     % "4.1.0",
  "org.scalatest"    %% "scalatest" % "3.2.18" % "test",
)
