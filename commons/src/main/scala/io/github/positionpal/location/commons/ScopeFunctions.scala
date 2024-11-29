package io.github.positionpal.location.commons

/** A set of scope functions (inspired by Kotlin) to improve the readability of the code. */
object ScopeFunctions:

  extension [A](a: A)
    /** @return `this` after having applied the given function [[f]] to it. */
    inline def also(f: A => Unit): A =
      f(a)
      a

    /** @return the result of applying the given function [[f]] to `this`. */
    inline def let[T](f: A => T): T = f(a)
