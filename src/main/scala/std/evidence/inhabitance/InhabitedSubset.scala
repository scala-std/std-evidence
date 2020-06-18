package std.evidence.inhabitance

import std.evidence.<~<

final case class InhabitedSubset[A, +B](conformity: A <~< B, inhabitance: Inhabited[A])
object InhabitedSubset {

}
