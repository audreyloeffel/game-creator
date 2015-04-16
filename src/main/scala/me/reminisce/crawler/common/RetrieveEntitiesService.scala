package me.reminisce.crawler.common

import akka.actor.{ActorRef, Props}
import me.reminisce.crawler.common.GraphResponses.{Paging, Root}
import me.reminisce.crawler.common.RetrieveEntitiesService._
import me.reminisce.server.domain.RestMessage
import org.json4s.jackson.JsonMethods._
import spray.client.pipelining._
import spray.http.StatusCodes._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}

object RetrieveEntitiesService {

  //Used for starting to crawl facebook
  //  case class RetrieveEntities(parameters: FBParameters)
  case class RetrieveEntities(params: FBSimpleParameters) extends RestMessage

  //Will be sent if not enough entities were found in the provided time span
  //facebook parameter since <-> until
  case class NotEnoughFound[A: TypeTag : ClassTag](entities: Vector[A])

  //Will be sent if the required minimum or more entities were found

  case class FinishedRetrievingEntities[A: TypeTag](entities: Vector[A]) {
    val tpe = typeOf[A]
  }

  case class PartialResult[A: TypeTag](entities: Vector[A]) {
    val tpe = typeOf[A]
  }


  private case class NotEnoughRetrieved[A](client: ActorRef,
                                           paging: Option[Paging],
                                           minimum: Int,
                                           count: Int = 0,
                                           entities: Vector[A] = Vector())

  private case class GetEntities[A](client: ActorRef,
                                    path: String,
                                    minimal: Int,
                                    count: Int = 0)


  def props[T](filter: (Vector[T]) => Vector[T])(implicit mf: Manifest[T]): Props =
    Props(new RetrieveEntitiesService[T](filter))


}

class RetrieveEntitiesService[T](filter: (Vector[T]) => Vector[T])(implicit mf: Manifest[T]) extends FBCommunicationManager {
  def receive = {
    case RetrieveEntities(params) =>
      val originalSender = sender()
      val path = s"$facebookPath/" + {
        params.query match {
          case Some(q) => q + {
            params.access_token match {
              case Some(a) => s"&access_token=$a"
              case None => ""
            }
          }
          case None => ""

        }
      }
      context.become(retrieveEntities())
      self ! GetEntities[T](originalSender, path, params.minimalEntities)
  }

  def retrieveEntities(): Receive = {
    case GetEntities(client, path, minimum, count) =>
      log.debug(s"Retriever path: $path")
      val responseF = pipelineRawJson(Get(path))
      responseF.onComplete {
        case Success(r) =>
          r.status match {
            case OK =>
              val json = parse(r.entity.asString)
              val root = json.extract[Root[List[T]]] match {
                case Root(None, _, _) =>
                  val root = json.extract[Root[T]]
                  root.data match {
                    case Some(data) => Root(Option(List(data)), root.paging)
                    case None => Root(None, root.paging)
                  }
                case _@result => result
              }

              val single = root match {
                case Root(None, _, _) =>
                  try {
                    Some(json.extract[T])
                  } catch {
                    case e: Exception =>
                      None
                  }
                case _ => None
              }
              var newEntities: Vector[T] = Vector.empty

              //As facebooks response isn't well structured, there are multiple ways the a potential
              //usable object can be constructed
              single match {
                case None =>
                  newEntities = filter(root.data.getOrElse(List[T]()).toVector)
                case Some(entity) =>
                  newEntities = filter(Vector(entity))
              }

              val newCount = count + newEntities.length
              if (newCount < minimum || minimum == 0) {
                self ! NotEnoughRetrieved(client, root.paging, minimum, newCount, newEntities)
              } else {
                client ! FinishedRetrievingEntities[T](newEntities)
              }
            case BadRequest => log.error(s"Facebook gave bad request for path: $path")
              client ! NotEnoughFound(Vector[T]())
            case _ =>
              client ! NotEnoughFound(Vector[T]())
              log.error(s"Can't retrieve entities due to unknown error ${r.status}")
          }
        case Failure(error) =>
          log.error(s"Facebook didn't respond \npath:$path\n  ${error.toString}")
          client ! NotEnoughFound(Vector[T]())
          context.become(receive)
      }
    case NotEnoughRetrieved(client, paging, minimum, count, entities) =>
      paging match {
        case Some(p) => p.next match {
          case Some(next) =>
            self ! GetEntities(client, next, minimum, count)
            client ! PartialResult(entities)
          case None =>
            if (minimum == 0) {
              client ! FinishedRetrievingEntities(entities)
            } else {
              log.info(s"Not enough found end of paging")
              client ! NotEnoughFound(entities)
            }
            context.become(receive)
        }
        case None =>
          if (minimum == 0) {
            client ! FinishedRetrievingEntities(entities)
          } else {
            log.info(s"Not enough found end of paging")
            client ! NotEnoughFound(entities)
          }
          context.become(receive)
      }
  }

}
