val scala3Version = "3.3.0"
val dfhdlVersion  = "0.2.28-SNAPSHOT"

lazy val root = project
    .in(file("."))
    .settings(
      name    := "dfhdl_template",
      version := "0.1.0",
      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding",
        "UTF-8",
        "-feature",
        "-language:strictEquality",
        "-language:implicitConversions"
      ),
      scalaVersion := scala3Version,
      addCompilerPlugin(
        "io.github.dfianthdl" % "dfhdl-plugin" % dfhdlVersion cross CrossVersion.full
      ),
      libraryDependencies += "io.github.dfianthdl" % "dfhdl_3" % dfhdlVersion
    )
