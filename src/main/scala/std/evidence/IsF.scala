package std.evidence

import std.evidence.inhabitance.Proposition
import std.evidence.internal.Unsafe


sealed abstract class IsF[F[X <: F[X]], A <: F[A], B <: F[B]] private[IsF]()  { ab =>
  import IsF._

  /**
    * To create an instance of `IsF[A, B]` you must show that for every
    * choice of `G[X <: F[X]]` you can convert `F[A]` to `F[B]`.
    */
  def subst[G[X <: F[X]]](fa: G[A]): G[B]

  /**
    * Substitution on identity brings about a direct coercion function of the
    * same form that `=:=` provides.
    *
    * @see [[coerce]]
    */
  def apply(a: A): B = coerce(a)

  /**
    * Substitution on identity brings about a direct coercion function of the
    * same form that [[=:=]] provides.
    *
    * @see [[apply]]
    */
  def coerce(a: A): B = {
    type f[a] = a
    subst[f](a)
  }

  /**
    * Given `A === B` we can convert `(X => A)` into `(X => B)`.
    */
  def onF[X](fa: X => A): X => B = {
    type f[a] = X => a
    subst[f](fa)
  }

  /**
    * Equality is transitive relation and its witnesses can be composed
    * in a chain much like functions.
    *
    * @see [[compose]]
    */
  def andThen[C <: F[C]](bc: IsF[F, B, C]): IsF[F, A, C] = {
    type f[x <: F[x]] = IsF[F, A, x]
    bc.subst[f](ab)
  }

  /**
    * Equality is transitive relation and its witnesses can be composed
    * in a chain much like functions.
    *
    * @see [[andThen]]
    */
  def compose[Z <: F[Z]](za: IsF[F, Z, A]): IsF[F, Z, B] =
    za.andThen(ab)

  /**
    * Equality is symmetric relation and therefore can be flipped around.
    * Flipping is its own inverse, so `x.flip.flip == x`.
    */
  def flip: IsF[F, B, A] = {
    type f[x <: F[x]] = IsF[F, x, A]
    subst[f](refl[F, A])
  }

  /**
    * Given `IsF[F, A, B]` we can prove that `IsF[H, G[A], G[B]]`.
    *
    * @see [[IsF.lift]]
    */
  def liftF[H[X <: H[X]], G[X <: F[X]] <: H[G[X]]]: IsF[H, G[A], G[B]] =
    IsF.liftF[H, G, F, A, B](ab)


  /**
    * Given `IsF[F, A, B]` we can prove that `G[A] === G[B]`.
    *
    * @see [[Is.lift]]
    * @see [[Is.lift2]]
    */
  def lift[G[X]]: G[A] === G[B] =
    IsF.lift[G, F, A, B](ab)

  /**
    * A value `IsF[F, A, B]` is always sufficient to produce a similar [[=:=]]
    * value.
    */
  def toPredef: A =:= B = {
    type f[a] = A =:= a
    subst[f](implicitly[A =:= A])
  }

  /**
    * Given `IsF[A, B]`, prove `A === B`.
    */
  def toIs: A === B = {
    type f[a] = A === a
    subst[f](Is.refl[A])
  }

  /**
    * Given `IsF[A, B]`, prove `A <~< B`.
    */
  def toAs: A <~< B = {
    type f[a] = A <~< a
    subst[f](As.refl[A])
  }
}

object IsF {
  implicit def proposition[F[X <: F[X]], A <: F[A], B <: F[B]]: Proposition[IsF[F, A, B]] =
    (p: ¬¬[IsF[F, A, B]]) => Axioms.isFConsistency[F, A, B](p.run)

  def apply[F[X <: F[X]], A <: F[A], B <: F[B]](implicit ev: IsF[F, A, B]): IsF[F, A, B] = ev

  private[this] final case class Refl[F[X <: F[X]], A <: F[A]]() extends IsF[F, A, A] {
    def subst[G[X <: F[X]]](x: G[A]): G[A] = x
  }

  /**
    * Equality is reflexive relation.
    */
  implicit def refl[F[X <: F[X]], A <: F[A]]: IsF[F, A, A] = new Refl[F, A]

  /**
    * Given `IsF[F, A, B]` we can prove that `G[A] === G[B]`.
    */
  def lift[G[X], F[X <: F[X]], A <: F[A], B <: F[B]]
  (ab: IsF[F, A, B]): Is[G[A], G[B]] = {
    type f[x <: F[x]] = Is[G[A], G[x]]
    ab.subst[f](Is.refl[G[A]])
  }

  /**
    * Given `IsF[F, A, B]` we can prove that `IsF[H, G[A], G[B]]`.
    */
  def liftF[H[X <: H[X]], G[X <: F[X]] <: H[G[X]], F[X <: F[X]], A <: F[A], B <: F[B]]
  (ab: IsF[F, A, B]): IsF[H, G[A], G[B]] = {
    type f[x <: F[x]] = IsF[H, G[A], G[x]]
    ab.subst[f](refl[H, G[A]])
  }

  /**
    * It can be convenient to convert a [[===]] value into a `IsF[F, A, B]` value.
    */
  def fromIs[F[X <: F[X]], A <: F[A], B <: F[B]](eq: A === B): IsF[F, A, B] = {
    type f[x <: F[x]] = IsF[F, A, x]
    Axioms.fBounded[F, A, B, f](eq, refl[F, A])
  }

  /**
    * It can be convenient to convert a [[=:=]] value into a `IsF[F, A, B]` value.
    */
  def fromPredef[F[X <: F[X]], A <: F[A], B <: F[B]](eq: A =:= B): IsF[F, A, B] =
    fromIs[F, A, B](Is.fromPredef(eq))
}