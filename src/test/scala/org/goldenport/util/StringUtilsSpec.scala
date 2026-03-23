package org.goldenport.util

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/*
 * @since   Dec. 22, 2025
 * @version Dec. 22, 2025
 * @author  ASAMI, Tomoharu
 */
class StringUtilsSpec extends AnyWordSpec
  with ScalaCheckDrivenPropertyChecks
  with Matchers {
  "StringUtils" should {
    "convert canonical names to kebab-case" in {
      StringUtils.toKebabCase("UserAccount") shouldBe "user-account"
      StringUtils.toKebabCase("AdminUser") shouldBe "admin-user"
      StringUtils.toKebabCase("APIService") shouldBe "api-service"
      StringUtils.toKebabCase("OAuthToken") shouldBe "oauth-token"
      StringUtils.toKebabCase("disableUser") shouldBe "disable-user"
      StringUtils.toKebabCase("resetPassword") shouldBe "reset-password"
    }

    "keep normalized values unchanged" in {
      StringUtils.toKebabCase("user-account") shouldBe "user-account"
      StringUtils.toKebabCase("user_account") shouldBe "user-account"
      StringUtils.toKebabCase(" user/account ") shouldBe "user-account"
    }
  }
}
