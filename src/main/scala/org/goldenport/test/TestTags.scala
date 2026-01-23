package org.goldenport.test

import org.scalatest.{Tag, Tagging}

/*
 * @since   Jan. 23, 2026
 * @version Jan. 23, 2026
 * @author  ASAMI, Tomoharu
 */
object TestTags {
  final val MANUAL_SPEC: String = "org.goldenport.tags.ManualSpec"
  final val FORK_SPEC: String = "org.goldenport.tags.ForkSpec"
}

trait ManualSpec extends Tagging {
  override def tags: Map[String, Tag] =
    super.tags + (TestTags.MANUAL_SPEC -> Tag(TestTags.MANUAL_SPEC))
}

trait ForkSpec extends Tagging {
  override def tags: Map[String, Tag] =
    super.tags + (TestTags.FORK_SPEC -> Tag(TestTags.FORK_SPEC))
}

trait ManualForkSpec extends ManualSpec with ForkSpec {
  override def tags: Map[String, Tag] =
    super[ManualSpec].tags ++ super[ForkSpec].tags
}
