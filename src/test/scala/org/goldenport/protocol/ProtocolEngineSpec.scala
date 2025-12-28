package org.goldenport.protocol

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import org.goldenport.test.ConsequenceMatchers
import org.goldenport.Consequence
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.observation.Cause
import org.goldenport.observation.Cause.Reason

/*
 * @since   Dec. 28, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
class ProtocolEngineSpec
  extends AnyWordSpec
     with Matchers
     with ConsequenceMatchers {

  "ProtocolEngine.makeOperationRequest" should {
    "positional arguments" when {
      "are valid" should {
        "succeed for a valid operation and arguments" in {
          val protocol = TestProtocol.simple
          val engine   = ProtocolEngine.create(protocol)

          val args = Array("query", "SimpleObject")

          val result = engine.makeOperationRequest(args)

          result should beSuccess
        }
      }
      "are missing" should {
        "fail with SyntaxError when no arguments are given" in {
          val protocol = TestProtocol.simple
          val engine   = ProtocolEngine.create(protocol)

          val args = Array.empty[String]

          val result = engine.makeOperationRequest(args)

          result should fail_syntax_error_with()
        }
        "fail with Missing argument when required parameter is absent" in {
          val protocol = TestProtocol.simple
          val engine   = ProtocolEngine.create(protocol)

          val args = Array("query")

          val result = engine.makeOperationRequest(args)

          result should fail_argument_missing_with(
            operation = "query",
            input = "target"
          )
        }
      }
      "are redundant" should {
        "fail with Redundant argument when too many arguments are given" in {
          val protocol = TestProtocol.simple
          val engine   = ProtocolEngine.create(protocol)

          val args = Array("query", "a", "b")

          val result = engine.makeOperationRequest(args)

          result should fail_argument_redundant_with(
            operation = "query"
          )
        }
      }
      "are invalid" should {
        "fail with Invalid argument when operation is unknown" in {
          val protocol = TestProtocol.simple
          val engine   = ProtocolEngine.create(protocol)

          val args = Array("unknown")

          val result = engine.makeOperationRequest(args)

          result should fail_argument_validation_error
        }
      }
    }
    "switch options and properties" when {
      "are provided" should {
        "include switches and properties from args" in {
          val protocol = TestProtocol.withArgsIngress
          val engine   = ProtocolEngine.create(protocol)

          val args = Array("query", "SimpleObject", "--dry-run", "--timeout=30")

          val result = engine.makeOperationRequest(args)

          result match {
            case Consequence.Success(opreq: OperationRequest.Core.Holder) =>
              opreq.switches.map(_.name) should contain ("dry-run")
              opreq.properties.map(p => (p.name, p.value)) should contain (("timeout", "30"))
            case Consequence.Success(_) =>
              fail("unexpected operation request type")
            case _ =>
              fail("makeOperationRequest should not fail")
          }
        }
      }
    }
  }
}
