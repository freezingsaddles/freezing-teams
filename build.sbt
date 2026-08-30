name           := "Freezing teams"
normalizedName := "freezing-teams"

description := "Team assignment tool."

version := "0.3"

organization         := "org.freezingsaddles"
organizationName     := "Freezing Saddles"
organizationHomepage := Some(uri("https://freezingsaddles.org"))

licenses += ("Apache-2.0", uri("https://www.apache.org/licenses/LICENSE-2.0.txt"))

scalaVersion := "3.8.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Werror",
  "-encoding",
  "UTF-8",
)

libraryDependencies ++= Seq(
  "com.learningobjects"    %% "scaloi"                     % "0.3.1",
  "com.nrinaudo"           %% "kantan.csv"                 % "0.8.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0",
).map(_.cross(CrossVersion.for3Use2_13))

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt"     % "4.1.0",
  "org.scalatest"    %% "scalatest" % "3.2.20" % "test",
)

// Patched versions of vulnerable transitive dependencies of org.scala-lang:scaladoc;
// removable once scaladoc catches up
dependencyOverrides ++= Seq(
  "org.jsoup"                  % "jsoup"            % "1.23.2",
  "com.fasterxml.jackson.core" % "jackson-core"     % "2.21.6",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.21.6",
  "tools.jackson.core"         % "jackson-core"     % "3.1.5",
  "tools.jackson.core"         % "jackson-databind" % "3.1.5",
)
