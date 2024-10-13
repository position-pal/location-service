package io.github.positionpal.location.commons

object ScopeFunctions:

  extension [A](a: A) def also(f: A => Unit): A = { f(a); a }
