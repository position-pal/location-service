package io.github.positionpal.location.domain

import io.github.positionpal.entities.NotificationMessage

/** Creates a new notification message with the given [[title]] and [[body]].
  * @return a new [[NotificationMessage]].
  */
def notification(title: String, body: String): NotificationMessage = NotificationMessage.create(title, body)

extension (n: NotificationMessage)
  /** @return a new [[NotificationMessage]] with the given [[prefix]] prepended to the title and body. */
  def prepended(prefix: String): NotificationMessage = notification(s"$prefix${n.title}", s"$prefix${n.body}")
