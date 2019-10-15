import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

import io.gatling.core.Predef.{Simulation, _}
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._

class ReservationSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .inferHtmlResources()
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .contentTypeHeader("application/json")

  val request = """{"start": "startDateValue", "end": "endDateValue", "userName":"js", "userEmail":"js" }"""
  val modif = """{"start": startDateValue, "end": endDateValue }"""

  def requestString(): String = {
    val randomReservationStart = randomStartDate()
    val randomReservationEnd = randomReservationStart.plus(scala.util.Random.nextInt(3), DAYS)
    val str = request.replace("startDateValue", randomReservationStart.toString)
      .replace("endDateValue", randomReservationEnd.toString)
    str
  }

  def modifString(): String = {
    val randomReservationStart = randomStartDate()
    val randomReservationEnd = randomReservationStart.plus(scala.util.Random.nextInt(3), DAYS)
    val str = modif.replace("startDateValue", randomReservationStart.toString)
      .replace("endDateValue", randomReservationEnd.toString)
    str
  }


  var randomReservationRequest = Iterator.continually(Map("randomRequest" -> requestString()))
  var randomModif = Iterator.continually(Map("randomModif" -> modifString()))

  def randomStartDate(): LocalDate = {
    LocalDate.now().plus(scala.util.Random.nextInt(30), DAYS)
  }

  def postReservationOne: HttpRequestBuilder = http("new_reservation_1")
    .post("/reservation")
    .body(StringBody("${randomRequest}"))
    .check(bodyString.saveAs("firstReservationId"))
    .check(status in (200, 400, 404))

  def postReservationTwo: HttpRequestBuilder = http("new_reservation_2")
    .post("/reservation")
    .body(StringBody("${randomRequest}"))
    .check(bodyString.saveAs("secondReservationId"))
    .check(status in (200, 400, 404))

  def putModifOne: HttpRequestBuilder = http("modif_reservation_1")
    .put(StringBody("/reservation/${firstReservationId}"))
    .body(StringBody("""${randomRequest}"""))
    .check(status in (200, 400, 404))

  def deleteReservationOne: HttpRequestBuilder = http("delete_reservation_1")
    .delete(StringBody("/reservation/${firstReservationId}"))
    .check(status in (200, 400, 404))

  def putModifTwo: HttpRequestBuilder = http("modif_reservation_2")
    .put(StringBody("/reservation/${secondReservationId}"))
    .body(StringBody("""${randomModif}"""))
    .check(status in (200, 400, 404))

  def deleteReservationTwo: HttpRequestBuilder = http("delete_reservation_2")
    .delete(StringBody("/reservation/${secondReservationId}"))
    .check(status in (200, 400, 404))

  val scn = scenario("ReservationSimulation")
    .during(30 seconds) {
      exec(feed(randomReservationRequest))
        .exec(feed(randomModif))
        .exec(postReservationOne)
        .exec(postReservationTwo)
        .exec(putModifOne)
        .exec(putModifTwo)
        .exec(deleteReservationOne)
        .exec(deleteReservationTwo)
    }

    setUp(
    scn.inject(
      rampConcurrentUsers(10) to (200) during (60 seconds) // 2
    )
  ).maxDuration(60 seconds)
    .protocols(httpProtocol)

}
