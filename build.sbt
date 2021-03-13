name := "Freezing teams"
normalizedName := "freezing-teams"

description := "Team assignment tool."

version := "0.2"

organization := "org.freezingsaddles"
organizationName := "Freezing Saddles"
organizationHomepage := Some(url("https://freezingsaddles.org"))

licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "com.learningobjects" %% "scaloi"     % "0.3.1",
  "com.nrinaudo"        %% "kantan.csv" % "0.6.1",
  "com.github.scopt"    %% "scopt"      % "4.0.1"
)
