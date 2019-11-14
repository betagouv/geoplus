name := """geoplus"""
organization := "beta.gouv.fr"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

PlayKeys.devSettings := Seq("play.server.http.port" -> "80")

scalaVersion := "2.13.1"

libraryDependencies ++= guice :: ws :: Nil
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "beta.gouv.fr.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "beta.gouv.fr.binders._"
