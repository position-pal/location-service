package io.github.positionpal.location.commons

import cats.mtl.{Ask, Raise, Stateful}

/** A type alias for a type constructor `M` expressing the capability to raise an error of type `E`. */
type CanRaise[E] = [M[_]] =>> Raise[M, E]

/** A type alias for a type constructor `M` expressing the capability to ask for a value of type `E`. */
type CanAsk[E] = [M[_]] =>> Ask[M, E]

/** A type alias for a type constructor `M` expressing the capability to have a state of type `S`. */
type HasState[S] = [M[_]] =>> Stateful[M, S]
