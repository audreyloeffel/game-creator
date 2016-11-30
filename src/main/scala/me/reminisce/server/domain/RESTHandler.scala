package me.reminisce.server.domain

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{OneForOneStrategy, _}
import me.reminisce.fetching.FetcherService.AckFetching
import me.reminisce.gameboard.questions.QuestionGenerator.FinishedQuestionCreation
import me.reminisce.server.domain.Domain.{ActionForbidden, Error, FailedBlacklist}
import me.reminisce.server.domain.RESTHandler.{Confirmation, WithActorRef, WithProps}
import me.reminisce.server.jsonserializer.GameCreatorFormatter
import spray.http.StatusCode
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.RequestContext

import scala.concurrent.duration._

/**
  * Defines an actor handling rest messages
  */
trait RESTHandler extends Actor with Json4sSupport with ActorLogging with GameCreatorFormatter {

  import context._

  def r: RequestContext

  def target: ActorRef

  def message: RestMessage

  setReceiveTimeout(10.seconds)
  // sends the request to an actor
  target ! message

  /**
    * Defines the way rest messages are handled (translated to a call to the
    * [[me.reminisce.server.domain.RESTHandler.complete]] method)
    *
    * @return
    */
  def receive = {
    case FinishedQuestionCreation(q) => complete(OK, q)
    case error: Error => complete(InternalServerError, error)
    case notFound: Domain.NotFound => complete(NotFound, notFound)
    case tooMany: Domain.TooManyRequests => complete(TooManyRequests, tooMany)
    case v: Domain.Validation => complete(BadRequest, v)
    case ReceiveTimeout => complete(GatewayTimeout, Domain.Error("Request timeout"))
    case graphUnreachable: Domain.GraphAPIUnreachable => complete(GatewayTimeout, graphUnreachable)
    case graphInvalidToken: Domain.GraphAPIInvalidToken => complete(Unauthorized, graphInvalidToken)
    case internalError: Domain.InternalError => complete(InternalServerError, internalError)
    case forbidden: ActionForbidden => complete(Forbidden, forbidden)
    case blacklistFail: FailedBlacklist => complete(InternalServerError, blacklistFail)
    case res: RestMessage =>
      complete(OK, res)
    case res@AckFetching(message) =>
      delayedComplete(OK, res)
    case x =>
      log.info("Per request received strange message " + x)
      complete(InternalServerError, "The request resulted in an unexpected answer.")
  }

  /**
    * Complete the request with the given message and status. Kills the worker.
    *
    * @param status status of the response
    * @param obj    response content
    * @tparam T type of the content
    */
  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    r.complete(status, obj)
    done()
  }

  /**
    * Complete the request with the given message and status. Then wait for the worker to ack the computation before
    * killing it.
    *
    * @param status status of the response
    * @param obj    response content
    * @tparam T type of the content
    */
  def delayedComplete[T <: AnyRef](status: StatusCode, obj: T) = {
    r.complete(status, obj)
    setReceiveTimeout(300.seconds)
    context.become(waitForConfirmation)
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e =>
        complete(InternalServerError, Domain.Error(e.getMessage))
        Stop
    }

  /**
    * Stops this actor and its children
    */
  def done(): Unit = {
    stop(self)
    target ! PoisonPill
  }

  /**
    * Waits for the worker then stops it
    */
  def waitForConfirmation: Receive = {
    case Confirmation(message) =>
      log.info(message)
      done()
    case x: Any =>
      log.info(s"Unexpected message received. $x")
  }
}

/**
  * [[me.reminisce.server.domain.RESTHandler]] implementations
  */
object RESTHandler {

  case class WithActorRef(r: RequestContext, target: ActorRef, message: RestMessage) extends RESTHandler

  case class WithProps(r: RequestContext, props: Props, message: RestMessage) extends RESTHandler {
    lazy val target = context.actorOf(props)
  }

  case class Confirmation(message: String)

}

/**
  * Factory for [[me.reminisce.server.domain.RESTHandler]]
  */
trait RESTHandlerCreator {
  this: Actor =>

  /**
    * Creates a rest handler
    *
    * @param r       request context to handle
    * @param target  target actor to handle the request
    * @param message request to send
    * @return reference to created rest handler
    */
  def perRequest(r: RequestContext, target: ActorRef, message: RestMessage) =
  context.actorOf(Props(WithActorRef(r, target, message)))

  /**
    * Creates a rest handler
    *
    * @param r       request context to handle
    * @param props   props for the target actor to handle the request
    * @param message request to send
    * @return reference to created rest handler
    */
  def perRequest(r: RequestContext, props: Props, message: RestMessage) =
  context.actorOf(Props(WithProps(r, props, message)))
}