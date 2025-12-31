package org.goldenport.context

import java.util.Locale

/*
 * @since   Dec. 31, 2025
 * @version Dec. 31, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class I18nContext extends I18nContext.Core.Holder {
}

object I18nContext {
  final case class Core(
    textNormalizationPolicy: String,
    textComparisonPolicy: String,
    dateTimeFormatPolicy: String,
    locale: Option[Locale]
  )

  object Core {
    trait Holder {
      def core: Core

      def textNormalizationPolicy: String = core.textNormalizationPolicy
      def textComparisonPolicy: String = core.textComparisonPolicy
      def dateTimeFormatPolicy: String = core.dateTimeFormatPolicy
      def locale: Option[Locale] = core.locale
    }
  }

  final case class Instant(core: Core) extends I18nContext {
  }
}
