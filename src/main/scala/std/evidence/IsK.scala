package std.evidence

import std.evidence.inhabitance.Proposition
import std.evidence.internal.Unsafe

/**
  * The existence of a value of type `IsK[A, B]` implies that A ≡ B.
  * For an explanation see [[Is]].
  *
  * @see [[=~=]] `A =~= B` is a type synonym to `LeibnizK[A, B]`
  */
sealed abstract class IsK[A[_], B[_]] private[IsK]() { ab =>
  import IsK._

  /**
    * To create an instance of `A =~= B` you must show that
    * for every choice of [[F]] you can convert `F[A]` to `F[B]`.
    */
  def subst[F[_[_]]](fa: F[A]): F[B]

  /**
    * Substitution on identity brings about a direct coercion function of the
    * same form that `=:=` provides.
    *
    * @see [[coerce]]
    */
  final def apply[X](a: A[X]): B[X] =
    coerce[X](a)

  /**
    * Substitution on identity brings about a direct coercion function of the
    * same form that [[=:=]] provides.
    *
    * @see [[apply]]
    */
  final def coerce[X](a: A[X]): B[X] = {
    type f[a[_]] = a[X]
    subst[f](a)
  }

  /**
    * Equality is transitive relation and its witnesses can be composed
    * in a chain much like functions.
    *
    * @see [[compose]]
    */
  final def andThen[C[_]](bc: B =~= C): A =~= C = {
    type f[b[_]] = A =~= b
    bc.subst[f](ab)
  }

  /**
    * Equality is transitive relation and its witnesses can be composed
    * in a chain much like functions.
    *
    * @see [[andThen]]
    */
  final def compose[Z[_]](za: Z =~= A): Z =~= B =
    za.andThen(ab)

  /**
    * Equality is symmetric relation and therefore can be flipped around.
    * Flipping is its own inverse, so `x.flip.flip == x`.
    */
  final def flip: B =~= A = {
    type f[a[_]] = a =~= A
    ab.subst[f](refl)
  }

  /**
    * Given `A =~= B` we can prove that `F[A] === F[B]`.
    *
    * @see [[IsK.lower]]
    * @see [[IsK.lower2]]
    */
  final def lower[F[_[_]]]: F[A] === F[B] =
    IsK.lower[F, A, B](ab)

  /**
    * Given `A =~= B` and `I =~= J` we can prove that `F[A, I] === F[B, J]`.
    *
    * @see [[IsK.lower]]
    * @see [[IsK.lower2]]
    * @see [[IsK.lower3]]
    */
  final def lower2[F[_[_], _[_]]]: PartiallyAppliedLower2[F] =
    new PartiallyAppliedLower2[F]
  final class PartiallyAppliedLower2[F[_[_], _[_]]] {
    def apply[I[_], J[_]](ij: I =~= J): F[A, I] === F[B, J] =
      IsK.lower2[F, A, B, I, J](ab, ij)
  }

  /**
    * Given `A =~= B` we can prove that `F[A, ?] =~= F[B, ?]`.
    *
    * @see [[IsK.lift]]
    * @see [[IsK.lift2]]
    */
  final def lift[F[_[_], _]]: F[A, ?] =~= F[B, ?] =
    IsK.lift(ab)

  /**
    * Given `A =~= B` and `I =~= J` we can prove that
    * `F[A, I, ?] =~= F[B, J, ?]`.
    *
    * @see [[IsK.lift]]
    * @see [[IsK.lift2]]
    * @see [[IsK.lift3]]
    */
  final def lift2[F[_[_], _[_], _]]: PartiallyAppliedLift2[F] =
    new PartiallyAppliedLift2[F]
  final class PartiallyAppliedLift2[F[_[_], _[_], _]] {
    def apply[I[_], J[_]](ij: I =~= J): F[A, I, ?] =~= F[B, J, ?] =
      IsK.lift2(ab, ij)
  }
}

object IsK {
  private[this] final case class Refl[A[_]]() extends IsK[A, A] {
    def subst[F[_[_]]](fa: F[A]): F[A] = fa
  }

  implicit def proposition[A[_], B[_]]: Proposition[IsK[A, B]] =
    (p: ¬¬[IsK[A, B]]) => Axioms.isKConsistency[A, B](p.run)

  def apply[A[_], B[_]](implicit ab: A =~= B): A =~= B = ab

  /**
    * Equality is reflexive relation.
    */
  implicit def refl[A[_]]: A =~= A = new Refl[A]()

  /**
    * Given `A =~= B` we can prove that `F[A] === F[B]`.
    *
    * @see [[lift2]]
    * @see [[lift3]]
    */
  def lower[F[_[_]], A[_], B[_]]
  (ab: A =~= B): F[A] === F[B] = {
    type f[a[_]] = F[A] === F[a]
    ab.subst[f](Is.refl)
  }

  def const[A, B](ab: A === B): λ[x => A] =~= λ[x => B] = {
    type f[a] = λ[x => A] =~= λ[x => a]
    ab.subst[f](IsK.refl[λ[x => A]])
  }

  /**
    * Given `A =~= B` and `I =~= J` we can prove that
    * `F[A, I] === F[B, J]`.
    *
    * @see [[lift]]
    * @see [[lift3]]
    */
  def lower2[F[_[_], _[_]], A[_], B[_], I[_], J[_]]
  (ab: A =~= B, ij: I =~= J): F[A, I] === F[B, J] = {
    type f1[a[_]] = F[A, I] === F[a, I]
    type f2[i[_]] = F[A, I] === F[B, i]
    ij.subst[f2](ab.subst[f1](Is.refl))
  }

  /**
    * Given `A =~= B`, `I =~= J`, and `M =~= N` we can prove that
    * `F[A, I] === F[B, J]`.
    *
    * @see [[lift]]
    * @see [[lift2]]
    */
  def lower3[F[_[_], _[_], _[_]], A[_], B[_], I[_], J[_], M[_], N[_]]
  (ab: A =~= B, ij: I =~= J, mn: M =~= N): F[A, I, M] === F[B, J, N] = {
    type f1[a[_]] = F[A, I, M] === F[a, I, M]
    type f2[i[_]] = F[A, I, M] === F[B, i, M]
    type f3[j[_]] = F[A, I, M] === F[B, J, j]
    mn.subst[f3](ij.subst[f2](ab.subst[f1](Is.refl)))
  }

  /**
    * Given `A =~= B` we can prove that `F[A, ?] =~= F[B, ?]`.
    *
    * @see [[lift2]]
    * @see [[lift3]]
    */
  def lift[F[_[_], ?], A[_], B[_]]
  (ab: A =~= B): F[A, ?] =~= F[B, ?] = {
    type f[a[_]] = F[A, ?] =~= F[a, ?]
    ab.subst[f](refl[F[A, ?]])
  }

  /**
    * Given `A =~= B` and `I =~= J` we can prove that
    * `F[A, I, ?] =~= F[B, J, ?]`.
    *
    * @see [[lift]]
    * @see [[lift3]]
    */
  def lift2[F[_[_], _[_], _], A[_], B[_], I[_], J[_]]
  (ab: A =~= B, ij: I =~= J): F[A, I, ?] =~= F[B, J, ?] = {
    type f1[a[_]] = F[A, I, ?] =~= F[a, I, ?]
    type f2[i[_]] = F[A, I, ?] =~= F[B, i, ?]
    ij.subst[f2](ab.subst[f1](refl[F[A, I, ?]]))
  }

  /**
    * Given `A =~= B`, `I =~= J`, and `M =~= N` we can prove that
    * `F[A, I, ?] =~= F[B, J, ?]`.
    *
    * @see [[lift]]
    * @see [[lift2]]
    */
  def lift3[F[_[_], _[_], _[_], _], A[_], B[_], I[_], J[_], M[_], N[_]]
  (ab: A =~= B, ij: I =~= J, mn: M =~= N): F[A, I, M, ?] =~= F[B, J, N, ?] = {
    type f1[a[_]] = F[A, I, M, ?] =~= F[a, I, M, ?]
    type f2[i[_]] = F[A, I, M, ?] =~= F[B, i, M, ?]
    type f3[j[_]] = F[A, I, M, ?] =~= F[B, J, j, ?]
    mn.subst[f3](ij.subst[f2](ab.subst[f1](refl[F[A, I, M, ?]])))
  }
}