import sbtassembly.AssemblyPlugin.defaultUniversalScript

ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.1.0"
ThisBuild / organization := "jestfs"

// Scala options
ThisBuild / scalacOptions := Seq(
  "-language:implicitConversions", // allow implicit conversions
  "-deprecation", // emit warning and location for usages of deprecated APIs
  "-explain", // explain errors in more detail
  "-explain-types", // explain type errors in more detail
  "-feature", // emit warning for features that should be imported explicitly
  "-unchecked", // enable warnings where generated code depends on assumptions
)

// Java options
ThisBuild / javacOptions ++= Seq(
  "-encoding",
  "UTF-8",
)

// automatic reload build.sbt
Global / onChangedBuildSource := ReloadOnSourceChanges

// Java options for assembly
lazy val assemblyJavaOpts = Seq(
  "-Xms1g",
  "-Xmx24g",
  "-Xss50m",
  "-XX:ReservedCodeCacheSize=512m",
  "-Dfile.encoding=utf8",
)

// assembly setting
ThisBuild / assemblyPrependShellScript := Some(
  assemblyJavaOpts.map("JAVA_OPTS=\"" + _ + " $JAVA_OPTS\"") ++
  defaultUniversalScript(shebang = false),
)

// project root
lazy val root = project
  .in(file("."))
  .settings(
    name := "jestfs",

    // libraries
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "org.apache.commons" % "commons-text" % "1.9",
      "org.jsoup" % "jsoup" % "1.14.3",
      "org.jline" % "jline" % "3.13.3",
      ("org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2")
        .cross(CrossVersion.for3Use2_13),
      "org.graalvm.js" % "js" % "22.2.0" % "provided",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    ),

    // Copy all managed dependencies to <build-root>/lib_managed/ This is
    // essentially a project-local cache.  There is only one lib_managed/ in
    // the build root (not per-project).
    retrieveManaged := true,

    // set the main class for 'sbt run'
    Compile / mainClass := Some("jestfs.JestFs"),

    // assembly setting
    assembly / test := {},
    assembly / assemblyOutputPath := file("bin/jestfs"),
  )

// create the `.completion` file for autocompletion in shell
lazy val genCompl = taskKey[Unit]("generate autocompletion file (.completion)")
genCompl := (root / Compile / runMain).toTask(" jestfs.util.GenCompl").value

// build for release with genCompl and assembly
lazy val release = taskKey[Unit]("release with genCompl and assembly")
release := {
  genCompl.value
  (root / assembly / assembly).value
}
