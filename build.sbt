organization := Settings.Organization
name         := "std-evidence"
version      := Settings.Version
licenses     += ("MIT", url("http://opensource.org/licenses/MIT"))

Settings.standalone := true
resolvers ++= {
  if (Settings.standalone.value) List(Resolver.mavenLocal)
  else Nil
}
libraryDependencies ++= {
  if (Settings.standalone.value) List[ModuleID](
    Settings.Organization %% "std-macro-illtyped"  % Settings.Version,
    Settings.Organization %% "std-data-iso"        % Settings.Version,
    Settings.Organization %% "std-data-quantified" % Settings.Version)
  else Nil
}

libraryDependencies ++= List(
  Dependencies.scalacheck % Test,
  Dependencies.scalaTest  % Test,
  Dependencies.macroCompat,
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided
)

// @silence annotation
libraryDependencies in ThisBuild ++= Seq(
  compilerPlugin(Dependencies.Plugin.silencerPlugin),
  Dependencies.Plugin.silencer)

publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
publishM2Configuration    := publishM2Configuration.value.withOverwrite(true)

autoCompilerPlugins := true
addCompilerPlugin(Dependencies.Plugin.macroParadise214)
addCompilerPlugin(Dependencies.Plugin.kindProjector)

scalaVersion      := "2.12.4-bin-typelevel-4"
scalaOrganization := {
  if (CrossVersion.partialVersion(scalaVersion.value).exists(_._2 >= 13))
    "org.scala-lang"
  else
    "org.typelevel"
}
licenses          += ("MIT", url("http://opensource.org/licenses/MIT"))

scalacOptions ++= List(
  "-deprecation",
  "-encoding", "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-Xfuture",
  "-Yliteral-types",
  "-Ypartial-unification",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  "-Yno-adapted-args",
  "-opt-warnings",
  "-Xlint:_,-type-parameter-shadow",
  "-Xsource:2.13",
  "-Ywarn-dead-code",
  "-Ywarn-extra-implicit",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused:_,-imports",
  "-Ywarn-value-discard",
  "-opt:l:inline",
  "-opt-inline-from:<source>")
