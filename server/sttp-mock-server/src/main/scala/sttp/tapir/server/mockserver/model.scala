package sttp.tapir.server.mockserver

import io.circe.JsonObject
import sttp.model.{MediaType, Method, StatusCode, Uri}

private[mockserver] final case class CreateExpectationRequest(
    httpRequest: ExpectationRequestDefinition,
    httpResponse: ExpectationResponseDefinition
)

private[mockserver] final case class VerifyExpectationRequest(
    httpRequest: ExpectationRequestDefinition,
    times: VerificationTimesDefinition
)

private[mockserver] final case class VerificationTimesDefinition(atMost: Option[Int], atLeast: Option[Int])

case class Expectation(
    id: String,
    priority: Int,
    httpRequest: ExpectationRequestDefinition,
    httpResponse: ExpectationResponseDefinition,
    times: ExpectationTimes,
    timeToLive: ExpectationTimeToLive
)

case class ExpectationRequestDefinition(
    method: Method,
    path: Uri,
    body: Option[ExpectationBodyDefinition],
    headers: Option[Map[String, List[String]]]
)

sealed trait ExpectationBodyDefinition extends Product with Serializable
object ExpectationBodyDefinition {
  private[mockserver] val PlainType: String = "STRING"
  private[mockserver] val JsonType: String = "JSON"

  case class Plain(string: String, contentType: MediaType) extends ExpectationBodyDefinition
  case class Json(json: JsonObject, matchType: JsonMatchType) extends ExpectationBodyDefinition

  sealed trait JsonMatchType {
    def entryName: String
  }
  object JsonMatchType {
    case object Strict extends JsonMatchType {
      override val entryName: String = "STRICT"
    }
    // todo: currently unused
    case object OnlyMatchingFields extends JsonMatchType {
      override val entryName: String = "ONLY_MATCHING_FIELDS"
    }
  }
}

sealed trait ExpectationMatched

case object ExpectationMatched extends ExpectationMatched

case class ExpectationResponseDefinition(body: Option[String], headers: Option[Map[String, List[String]]], statusCode: StatusCode)

case class ExpectationTimes(unlimited: Boolean, remainingTimes: Option[Int])

case class ExpectationTimeToLive(unlimited: Boolean, timeToLive: Option[Int], timeUnit: Option[String])

sealed trait VerificationTimes {
  protected[mockserver] def toDefinition: VerificationTimesDefinition
}

object VerificationTimes {
  val never: VerificationTimes = Impl(atMost = Some(0), atLeast = None)

  val exactlyOnce: VerificationTimes = exactly(times = 1)

  val atMostOnce: VerificationTimes = atMost(times = 1)

  val atLeastOnce: VerificationTimes = atLeast(times = 1)

  def exactly(times: Int): VerificationTimes = Impl(atMost = Some(times), atLeast = Some(times))

  def atMost(times: Int): VerificationTimes = Impl(atMost = Some(times), atLeast = None)

  def atLeast(times: Int): VerificationTimes = Impl(atMost = None, atLeast = Some(times))

  final case class Impl(atMost: Option[Int], atLeast: Option[Int]) extends VerificationTimes {
    override val toDefinition: VerificationTimesDefinition = VerificationTimesDefinition(atMost, atLeast)
  }
}
