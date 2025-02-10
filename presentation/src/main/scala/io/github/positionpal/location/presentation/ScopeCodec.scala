package io.github.positionpal.location.presentation

import io.github.positionpal.entities.{GroupId, UserId}

object ScopeCodec:
  import io.github.positionpal.location.domain.Scope

  private val separator = "_"

  extension (scope: Scope) def encode: String = s"${scope.userId.value()}$separator${scope.groupId.value()}"

  extension (s: String)
    def decode: Scope =
      val parts = s.split(separator)
      Scope(UserId.create(parts(0)), GroupId.create(parts(1)))
