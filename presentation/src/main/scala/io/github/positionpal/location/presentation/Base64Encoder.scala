package io.github.positionpal.location.presentation

import java.util.Base64

import io.github.positionpal.entities.{GroupId, UserId}

object Base64Encoder:
  extension (s: String) def encode: String = Base64.getEncoder.encodeToString(s.getBytes)

  extension (s: String) def decode: String = new String(Base64.getDecoder.decode(s))

object ScopeUtils:
  import io.github.positionpal.location.domain.Scope

  extension (scope: Scope) def concatenated: String = s"${scope.userId.value()}-${scope.groupId.value()}"

  extension (s: String)
    def splitted: Scope =
      val parts = s.split("-")
      Scope(UserId.create(parts(0)), GroupId.create(parts(1)))
