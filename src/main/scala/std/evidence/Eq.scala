package std.evidence

import std.evidence.internal.Unsafe

import scala.{specialized => sp}

trait Eq[@sp -A] extends Any with Serializable {
  /**
    * Returns `true` if `x` and `y` are equal, `false` otherwise.
    */
  def eqv(first: A, second: A): Boolean

  /**
    * Returns `false` if `x` and `y` are equal, `true` otherwise.
    */
  def neqv(first: A, second: A): Boolean = !eqv(first, second)

  /**
    * Returns either a proof that the singleton values are equal or
    * a proof that the values are different.
    */
  def compare(x: A, y: A): Either[x.type =!= y.type, x.type === y.type] =
    if (eqv(x, y)) Right(Unsafe.is[x.type, y.type])
    else Left(Unsafe.weakApart[x.type, y.type])
}

object Eq {
  import java.lang.Double.doubleToRawLongBits
  import java.lang.Float.floatToRawIntBits

  def apply[A](implicit A: Eq[A]): Eq[A] = A

  trait Univ[@sp A] extends Any with Eq[A]

  def fromUniversalEquals[A]: Univ[A] = new Univ[A] {
    override def eqv(first: A, second: A): Boolean =
      first == second
  }

  def propositionEq[A]: Eq[A] = new Eq[A] {
    override def eqv(x: A, y: A): Boolean = true
  }

  implicit val eqBool: Eq[Boolean] = new Eq[Boolean] {
    override def eqv(x: Boolean, y: Boolean): Boolean = x == y
  }

  implicit val eqByte: Eq[Byte] = new Eq[Byte] {
    override def eqv(x: Byte, y: Byte): Boolean = x == y
  }

  implicit val eqShort: Eq[Short] = new Eq[Short] {
    override def eqv(x: Short, y: Short): Boolean = x == y
  }

  implicit val eqInt: Eq[Int] = new Eq[Int] {
    override def eqv(x: Int, y: Int): Boolean = x == y
  }

  implicit val eqLong: Eq[Long] = new Eq[Long] {
    override def eqv(x: Long, y: Long): Boolean = x == y
  }

  implicit val eqChar: Eq[Char] = new Eq[Char] {
    override def eqv(x: Char, y: Char): Boolean =
      x == y
  }

  implicit val eqString: Eq[String] = new Eq[String] {
    override def eqv(x: String, y: String): Boolean =
      x == y
  }

  implicit val eqFloat:  Eq[Float] = new Eq[Float] {
    override def eqv(x: Float, y: Float): Boolean =
      floatToRawIntBits(x) == floatToRawIntBits(y)
  }

  implicit val eqDouble: Eq[Double] = new Eq[Double] {
    override def eqv(x: Double, y: Double): Boolean =
      doubleToRawLongBits(x) == doubleToRawLongBits(y)
  }

  implicit val eqUnit: Eq[Unit] = new Eq[Unit] {
    override def eqv(x: Unit, y: Unit): Boolean = true
  }

  implicit def option[T](implicit T: Eq[T]): Eq[Option[T]] = new Eq[Option[T]] {
    override def eqv(x: Option[T], y: Option[T]): Boolean =
      (x, y) match {
        case (None, None) => true
        case (Some(x), Some(y)) => T.eqv(x, y)
        case _ => false
      }
  }

  implicit def pair[A, B](implicit A: Eq[A], B: Eq[B]): Eq[(A, B)] = new Eq[(A, B)] {
    override def eqv(x: (A, B), y: (A, B)): Boolean =
      (x, y) match {
        case ((x1, x2), (y1, y2)) => A.eqv(x1, y1) && B.eqv(x2, y2)
        case _ => false
      }
  }

  implicit def either[A, B](implicit A: Eq[A], B: Eq[B]): Eq[Either[A, B]] = new Eq[Either[A, B]] {
    override def eqv(x: Either[A, B], y: Either[A, B]): Boolean =
      (x, y) match {
        case (null, null) => true
        case (Left(x), Left(y)) => A.eqv(x, y)
        case (Right(x), Right(y)) => B.eqv(x, y)
        case _ => false
      }
  }
}
