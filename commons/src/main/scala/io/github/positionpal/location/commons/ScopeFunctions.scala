package io.github.positionpal.location.commons

/** A set of scope functions (inspired by Kotlin) to improve the readability of the code. */
object ScopeFunctions:

  extension [A](a: A)
    /** @return `this` after having applied the given function [[f]] to it. */
    def also(f: A => Unit): A =
      f(a)
      a
