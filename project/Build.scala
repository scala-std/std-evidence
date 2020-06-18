import sbt.Keys._
import sbt._
import wartremover.WartRemover.autoImport.Wart

sealed trait FlagValue
final case class Enable(compileOnly: Boolean) extends FlagValue
final case object Disable extends FlagValue

object Settings {
  def setting[A](compileOnly: Boolean, setting: SettingKey[A]): SettingKey[A] =
    if (compileOnly) setting in (Compile, compile) else setting
  def setting[A](compileOnly: Boolean, setting: TaskKey[A]): TaskKey[A] =
    if (compileOnly) setting in (Compile, compile) else setting

  def forceAcyclicity(compileOnly: Boolean) = List(
    setting(compileOnly, libraryDependencies) +=
      "com.lihaoyi" %% "acyclic" % "0.2.0" % "provided",
    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.2.0"),
    setting(compileOnly, scalacOptions) += "-P:acyclic:force"
  )

  val warts = List(
    Wart.AnyVal,
    Wart.ArrayEquals,
    Wart.AsInstanceOf,
    Wart.EitherProjectionPartial,
    Wart.Enumeration,
    Wart.ExplicitImplicitTypes,
    Wart.FinalCaseClass,
    Wart.FinalVal,
    Wart.ImplicitConversion,
    Wart.IsInstanceOf,
    Wart.JavaSerializable,
    Wart.LeakingSealed,
    Wart.MutableDataStructures,
    Wart.NonUnitStatements,
    Wart.Null, // orNull and nulls are useful for exceptions...
    Wart.OptionPartial,
    Wart.Product,
    Wart.Return,
    Wart.Serializable,
    Wart.StringPlusAny,
    Wart.Throw,
    Wart.TraversableOps,
    Wart.TryPartial,
    Wart.Var,
    Wart.While)

  def forceFatalWarnings(compileOnly: Boolean) =
    if (compileOnly) List(scalacOptions in (Compile, compile) += "-Xfatal-warnings")
    else List(scalacOptions += "-Xfatal-warnings")

  def common
  (acyclic: FlagValue = Enable(false),
   fatalWarnings: FlagValue = Enable(false),
   enableWarts: FlagValue = Enable(false)
  ): List[Def.Setting[_]] = {
    val f1 = acyclic match {
      case Enable(x) => forceAcyclicity(x)
      case Disable => Nil
    }

    val f2 = fatalWarnings match {
      case Enable(x) => forceFatalWarnings(x)
      case Disable => Nil
    }

    val f3 = enableWarts match {
      case Enable(true) => List(wartremover.wartremoverWarnings in (Compile, compile) ++= warts)
      case Enable(false) => List(wartremover.wartremoverWarnings ++= warts)
      case Disable => Nil
    }

    f1 ++ f2 ++ f3
  }

  final val Organization: String = "com.alexknvl"
  final val Version: String = "0.1.0.0"

  lazy val standalone = settingKey[Boolean]("standalone")
}

object Dependencies {
  object Plugin {
    val kindProjector           = "org.typelevel"         %% "kind-projector"     % "0.10.3"
    val betterMonadicFor        = "com.olegpy"            %% "better-monadic-for" % "0.3.1"
    val silencerPlugin          = "com.github.ghik"       %% "silencer-plugin"    % "1.4.2"
    val silencer                = "com.github.ghik"       %% "silencer-lib"       % "1.4.2" % Provided intransitive()
    val splain                  = "io.tryp"                % "splain"             % "0.5.6" cross CrossVersion.patch
    val nonExhaustiveMatchError = "com.softwaremill.neme" %% "neme-plugin"        % "0.0.5"
    val macroParadise214        = "org.scalamacros"        % "paradise_2.12.4"    % "2.1.1"
  }

  val macroCompat = "org.typelevel" %% "macro-compat" % "1.1.1"

  val jline = "jline" % "jline" % "2.14.6"

  val bcel = "org.apache.bcel" % "bcel" % "6.4.0"

  val asm         = "org.ow2.asm" % "asm" % "8.0.1"
  val asmAnalysis = "org.ow2.asm" % "asm-analysis" % "8.0.1"
  val asmTree     = "org.ow2.asm" % "asm-tree" % "8.0.1"
  val asmUtil     = "org.ow2.asm" % "asm-util" % "8.0.1"
  val asmCommons  = "org.ow2.asm" % "asm-commons" % "8.0.1"

  val guava = "com.google.guava" % "guava" % "29.0-jre"

  val sourcecode = "com.lihaoyi" %% "sourcecode" % "0.1.7"

  val jacop = "org.jacop" % "jacop" % "4.7.0"
  val choco = "org.choco-solver" % "choco-solver" % "4.10.2"

  val universalCharDet = "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3"
  val commonsCodec     = "commons-codec" % "commons-codec" % "1.12"
  val univocityCsv     = "com.univocity" % "univocity-parsers" % "2.8.2"

  val jsoup            = "org.jsoup"  % "jsoup" % "1.12.1"
  val jodaTime         = "joda-time" % "joda-time" % "2.10.6"

  val fastutil         = "it.unimi.dsi" % "fastutil" % "8.2.2"

  val httpclient   = "org.apache.httpcomponents"      % "httpclient"          % "4.5.8"
  val jodaConvert  = "org.joda"                       % "joda-convert"        % "1.8.1"
  val opencsv      = "net.sf.opencsv"                 % "opencsv"             % "2.3"
  val scalajHttp   = "org.scalaj"   %% "scalaj-http" % "2.4.2"

  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.3.0"

  // val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % "0.21.4"
  // val http4sCirce       = "org.http4s" %% "http4s-circe"        % "0.21.4"
  // val http4sDsl         = "org.http4s" %% "http4s-dsl"          % "0.21.4"

  val parboiled              = "org.parboiled"          %% "parboiled-scala"          % "1.3.1"
  // val attoCore               = "org.tpolecat"           %% "atto-core"                % "0.8.0"
  val parsebackCore          = "com.codecommit"         %% "parseback-core"           % "0.3"
  val parsebackCats          = "com.codecommit"         %% "parseback-cats"           % "0.3"
  val scalaParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1"

//  val catsCore    = "org.typelevel"  %% "cats-core"    % "2.1.1"
//  val catsFree    = "org.typelevel"  %% "cats-free"    % "1.6.0"
//  val catsEffect  = "org.typelevel"  %% "cats-effect"  % "1.3.0"
//  val spire       = "org.typelevel"  %% "spire"        % "0.17.0-M1"
//  val catsAlgebra = "org.typelevel"  %% "algebra"      % "2.0.1"
//  val kittens     = "org.typelevel"  %% "kittens"      % "2.1.0"

  val zio         = "dev.zio"        %% "zio"          % "1.0.0-RC20"
  val zioStream   = "dev.zio"        %% "zio-streams"  % "1.0.0-RC20"
  val zioProcess  = "dev.zio"        %% "zio-process"  % "0.0.5"

  val magnolia    = "me.lyh"         %% "magnolia"     % "0.10.1-jto"

  // val jawn         = "org.typelevel" %% "jawn-ast" % "0.14.0"
  // val circeCore    = "io.circe" %% "circe-core"    % "0.13.0"
  // val circeGeneric = "io.circe" %% "circe-generic" % "0.13.0"
  // val circeParser  = "io.circe" %% "circe-parser"  % "0.13.0"

  // val decline     = "com.monovore"   %% "decline"      % "1.2.0"
  val asciiGraphs = "com.github.mdr" %% "ascii-graphs" % "0.0.6"
  val diffutils    = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0" % "test"

  // val discipline = "org.typelevel"  %% "discipline" % "0.11.1"
  val scalaTest  = "org.scalatest"  %% "scalatest"  % "3.0.7"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"
}
