package org.goldenport.provisional.presentation

import org.goldenport.provisional.conclusion.{Severity, Responsibility, UserAction}

case class PresentedConclusion(
  title: MessageRef,
  summary: MessageRef,
  severity: Severity,
  responsibility: Responsibility,
  userAction: Option[UserAction]
)
