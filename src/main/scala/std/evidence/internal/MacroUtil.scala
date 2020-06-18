package std.evidence.internal

import std.evidence.Eq
import std.evidence.inhabitance.{Contractible, Inhabited, SingletonOf}

import scala.reflect.macros.blackbox
import scala.reflect.macros.whitebox
import scala.tools.nsc.ast.NodePrinters
import scala.reflect.internal.Types

@SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
private[std] abstract class Shared[C <: blackbox.Context] {
  val c: C
  import c.universe._
  import definitions.{ NothingClass, AnyClass }

  final val NothingType: Type = NothingClass.toType
  final val AnyType: Type = AnyClass.toType
  final val AnyRefType: Type = typeOf[AnyRef]

  final val inhabitants: Map[Type, Tree] = Map(
    typeOf[Unit]    -> q"()",
    typeOf[Boolean] -> q"false",
    typeOf[Byte]    -> q"0.toByte",
    typeOf[Short]   -> q"0.toShort",
    typeOf[Int]     -> q"0",
    typeOf[Long]    -> q"0L",
    typeOf[Float]   -> q"0.0f",
    typeOf[Double]  -> q"0.0d",
    typeOf[Symbol]  -> q"'a",
    typeOf[String]  -> q""" "" """)

  final val EqType: Symbol = typeOf[Eq[_]].typeSymbol
  final val EqTypeConstructor: Type = typeOf[Eq[_]].typeConstructor

  def findCosingleton(tpe: Type, evType: Type): Option[(c.Tree, c.Type)] = {
    val EvType: Symbol = evType.typeSymbol
    val EvTypeConstructor: Type = evType.typeConstructor

    val exactEq = c.inferImplicitValue(appliedType(EvTypeConstructor, tpe), silent = true)

    if (exactEq != EmptyTree) {
      exactEq.tpe match {
        case TypeRef(_, EvType, List(result)) => Some((exactEq, result))
        case _ => None
      }
    } else None
  }

  /**
    * 0, null, "a", a.type | supported for types with Eq
    *
    * ExistentialType      | nominalRef[ExistentialType]("ExistentialType"): T[ExistentialType]
    * ClassName            | nominalRef[ClassName]("ClassName"): T[ClassName]
    * F[A]                 | nominalRef1[F, A]("F", T[A]): T[F[A]]
    * F[G]                 | nominalRef2[G, A]("F"
    *
    * A with B             | unsupported
    *
    * A { ... }            | unsupported
    * A forSome { ... }    | unsupported
    */
  trait TypeRefAlg[A] {
    def nominal(tpe: Type, name: String, args: List[A]): A
    def singleton(tpe: Type, parent: A, value: Tree, eq: Tree): A
  }
  def foldConcreteType[T](tpe: Type, evType: Type)(alg: TypeRefAlg[T]): T = tpe.dealias match {
    case t if (t <:< NothingType) && (NothingType <:< t) =>
      alg.nominal(NothingType, "scala.Nothing", Nil)

    case t if (t <:< AnyType) && (AnyType <:< t) =>
      alg.nominal(AnyType, "scala.Any", Nil)

    case tpe@SingleType(_, path) =>
      findCosingleton(tpe, evType) match {
        case Some((eq, cosingleton)) =>
          val parent = foldConcreteType[T](cosingleton, evType)(alg)
          alg.singleton(tpe, parent, q"$path.asInstanceOf[$tpe]", eq)

        case None =>
          c.abort(c.enclosingPosition, s"Could not widen a singleton $tpe: no Eq[$tpe] found.")
      }

    case tpe@ConstantType(value) =>
      findCosingleton(tpe, evType) match {
        case Some((eq, cosingleton)) =>
          val parent = foldConcreteType[T](cosingleton, evType)(alg)
          alg.singleton(tpe, parent, q"$value.asInstanceOf[$tpe]", eq)

        case None =>
          c.abort(c.enclosingPosition, s"Could not widen a singleton $tpe: no Eq[$tpe] found.")
      }

    case tpe: TypeRef if tpe.sym.isClass =>
      val args = tpe.typeArgs.map(foldConcreteType[T](_, evType)(alg))
      alg.nominal(tpe, tpe.typeSymbol.fullName, args)

    case x =>
      c.abort(c.enclosingPosition, s"$tpe is not a concrete type (${x.getClass}).")

//    case RefinedType(parents, decls) =>
//      // a with b { }
//      if(decls.nonEmpty)
//        c.abort(c.enclosingPosition, "Refinements with non-empty scope are not yet supported.")
//
//      parents.map(go).reduce(alg.intersection).asInstanceOf[T[A]]
  }

  def isConcreteType(tpe: Type): Boolean = tpe.dealias match {
    case t if (t <:< NothingType) && (NothingType <:< t) => true
    case t if (t <:< AnyType) && (AnyType <:< t) => true
    case SingleType(_, v) if !v.isParameter => true
    case ConstantType(_) => true
    case t: TypeRef if t.typeSymbol.isClass => t.typeArgs.forall(isConcreteType)
    case _ => false
  }

  final class Hidden1
  final class Hidden2

  def isConstant[F[_]](F: c.WeakTypeTag[F[_]]): Boolean = {
    val applied1 = c.universe.appliedType(F.tpe, weakTypeOf[Hidden1])
    val applied2 = c.universe.appliedType(F.tpe, weakTypeOf[Hidden2])
    applied1 =:= applied2
  }

  def isInjective[F[_]](F: c.WeakTypeTag[F[_]]): Boolean = {
    val applied1 = c.universe.appliedType(F.tpe, weakTypeOf[Hidden1])
    val applied2 = c.universe.appliedType(F.tpe, weakTypeOf[Hidden2])
    !(applied1 =:= applied2) && isConcreteType(applied1) && isConcreteType(applied2)
  }
}

private[std] final class Whitebox(val c: whitebox.Context) extends Shared[whitebox.Context] {
  import c.universe._
  import internal._
  import definitions.NothingClass

  def cosingleton[A : c.WeakTypeTag]: c.Tree = {
    val tpe = weakTypeOf[A]
    findCosingleton(tpe, typeOf[Eq[_]]) match {
      case Some((eqi, x)) =>
        q"""_root_.std.evidence.Cosingleton.witness[$x, $tpe]($eqi)"""
      case None =>
        c.abort(c.enclosingPosition, s"Could not find a cosingleton for $tpe.")
    }
  }
}

private[std] final class MacroUtil(val c: blackbox.Context) extends Shared[blackbox.Context] {
  import c.universe._
  import internal._

  def mkInhabited[A](implicit A: c.WeakTypeTag[A]): c.Tree =
    weakTypeOf[A].dealias match {
      case tpe@SingleType(_, path) =>
        q"""_root_.std.evidence.inhabitance.Inhabited.value[$tpe]($path.asInstanceOf[$tpe])"""
      case tpe@ConstantType(value) =>
        q"""_root_.std.evidence.inhabitance.Inhabited.value[$tpe]($value)"""
      case tpe@ThisType(_) =>
        q"""_root_.std.evidence.inhabitance.Inhabited.value[$tpe](this)"""

      case tpe =>
        inhabitants.find { case (t, _) => t <:< tpe } match {
          case Some((_, tree)) => q"""_root_.std.evidence.inhabitance.Inhabited.value[$tpe]($tree)"""
          case None => c.abort(c.enclosingPosition, s"Can't prove that $tpe is inhabited.")
        }
    }

  def mkUninhabited[A](implicit A: c.WeakTypeTag[A]): c.Tree =
    weakTypeOf[A].dealias match {
      case tpe if tpe <:< NothingType =>
        q"""_root_.std.evidence.inhabitance.Uninhabited.witness[$tpe](a => a)"""

      case tpe@TypeRef(pre, sym, args) =>
        // println(s"$pre $sym $args ${sym.isFinal} ${sym.isClass} ${sym.isPublic} ${sym.asClass.isSealed}")
        // println(s"${sym.asClass.toType.members}")
        c.abort(c.enclosingPosition, s"Can't prove that $tpe is uninhabited (yet).")

      case tpe =>
        // println(s"$tpe ${tpe.getClass}")
        c.abort(c.enclosingPosition, s"Can't prove that $tpe is uninhabited.")
    }

  def mkInjective[F[_]](implicit F: c.WeakTypeTag[F[_]]): c.Tree =
    if (isInjective[F](F))
      q"_root_.std.evidence.variance.Injective.force[${F.tpe}](_root_.std.evidence.internal.Unsafe.unsafe)"
    else
      c.abort(c.enclosingPosition, s"Can't prove that ${F.tpe} is injective.")

  def mkConstant[F[_]](implicit F: c.WeakTypeTag[F[_]]): c.Tree =
    if (isConstant[F](F))
      q"_root_.std.evidence.variance.Constant.force[${F.tpe}](_root_.std.evidence.internal.Unsafe.unsafe)"
    else
      c.abort(c.enclosingPosition, s"Can't prove that ${F.tpe} is injective.")

  def mkWeakApart[A : c.WeakTypeTag, B : c.WeakTypeTag]: c.Tree = {
    val ta = weakTypeOf[A]
    val tb = weakTypeOf[B]
    if (isConcreteType(ta) && isConcreteType(tb) && !(ta <:< tb && tb <:< ta)) {
      // val ca = makeConcreteType(ta)
      // val cb = makeConcreteType(tb)
      q"""_root_.std.evidence.internal.Unsafe.weakApart[$ta, $tb]"""
    } else {
      c.abort(c.enclosingPosition, s"Could not prove that $ta =!= $tb.")
    }
  }
}