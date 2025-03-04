package io.github.positionpal.location.domain

import io.github.positionpal.entities.NotificationMessage

def notification(title: String, body: String): NotificationMessage = NotificationMessage.create(title, body)

extension (n: NotificationMessage)
  def prepended(prefix: String): NotificationMessage = notification(s"$prefix${n.title}", s"$prefix${n.body}")
