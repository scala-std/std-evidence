package std.evidence

import std.evidence.inhabitance.Uninhabited
import std.evidence.inhabitance.Uninhabited.witness

object Void {
  private[evidence] trait Tag extends Any

  def absurd[A](v: Void): A = v

  val isNotUnit: Void =!= Unit =
    WeakApart.witness(_.flip.coerce(()))

  val isNotAny: Void =!= Any =
    WeakApart.witness(_.flip.coerce(()))

  implicit def uninhabited: Uninhabited[Void] =
    witness(identity[Void])
}