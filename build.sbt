name := "dfiant-template"

val dfiantVersion = "0.0.19"

/////////////////////////////////////////////////////////
// These lines should be placed in every DFiant project
/////////////////////////////////////////////////////////
version := "0.1"
scalaVersion := "2.13.1"
scalacOptions += "-deprecation"
scalacOptions ++= Seq("-feature")
scalacOptions ++= Seq("-language:reflectiveCalls")
scalacOptions ++= Seq("-language:existentials")
scalacOptions ++= Seq("-language:implicitConversions")
scalacOptions ++= Seq("-language:higherKinds")
libraryDependencies ++= Seq(
  "io.github.dfianthdl" %% "dfiant" % dfiantVersion
)
/////////////////////////////////////////////////////////
