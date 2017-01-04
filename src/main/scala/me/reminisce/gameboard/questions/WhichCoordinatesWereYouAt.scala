package me.reminisce.gameboard.questions

import akka.actor.Props
import akka.event.Logging
import com.github.nscala_time.time.Imports._
import me.reminisce.database.MongoCollections
import me.reminisce.database.MongoDBEntities.FBPost
import me.reminisce.gameboard.board.GameboardEntities._
import me.reminisce.gameboard.questions.QuestionGenerator.{CreateQuestion, FinishedQuestionCreation, MongoDBError, NotEnoughData}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Factory for [[me.reminisce.gameboard.questions.WhichCoordinatesWereYouAt]]
  */
object WhichCoordinatesWereYouAt {

  /**
    * Creates a WhichCoordinatesWereYouAt question generator
    *
    * @param database database from which to take the data
    * @return props for the created actor
    */
  def props(database: DefaultDB): Props =
    Props(new WhichCoordinatesWereYouAt(database))
}

/**
  * WhichCoordinatesWereYouAt question generator
  *
  * @param db database from which to take the data
  */
class WhichCoordinatesWereYouAt(db: DefaultDB) extends QuestionGenerator {
  override val log = Logging(context.system, this)

  /**
    * Entry point for this actor, handles the CreateQuestionWithMultipleItems(userId, itemIds) message by getting the
    * necessary items from the database and creating a question. If some items are non conform to what is expected,
    * missing or there is an error while contacting the database, the error is reported to the client.
    *
    * @return Nothing
    */
  def receive = {
    case CreateQuestion(userId, itemId) =>
      val client = sender()
      val query = BSONDocument(
        "userId" -> userId,
        "postId" -> itemId
      )
      val postCollection = db[BSONCollection](MongoCollections.fbPosts)
      postCollection.find(query).one[FBPost].onComplete {
        case Success(postOpt) =>
          val maybeQuestion =
            for {
              post <- postOpt
              place <- post.place
            } yield {
              val answer = place.location
              val postSubject = QuestionGenerator.subjectFromPost(post, includeStory = false)
              val subjectWithHiddenPlace = hidePlace(postSubject, post)
              GeolocationQuestion(userId, Geolocation, GeoWhatCoordinatesWereYouAt, Some(subjectWithHiddenPlace),
                answer)
            }
          maybeQuestion match {
            case Some(q) =>
              client ! FinishedQuestionCreation(q)
            case None =>
              client ! NotEnoughData(s"Post has no place or post does not exist : $itemId")
          }
        case Failure(e) =>
          client ! MongoDBError(s"${e.getMessage}")
        case any =>
          client ! MongoDBError(s"$any")
      }
    case any =>
      log.error(s"Wrong message received $any.")
  }

  private def hidePlace(subject: PostSubject, post: FBPost): PostSubject = {
    post.place.fold(subject) {
      place =>
        val noCountry = replaceNames(place.location.country.getOrElse(""), subject.text)
        val noCity = replaceNames(place.location.city.getOrElse(""), noCountry)
        val noStreet = replaceNames(place.location.street.getOrElse(""), noCity)
        val newText =
          if (noStreet != subject.text) {
            val timeFormatter = DateTimeFormat.forPattern("'on the 'yyyy-MM-dd' at 'HH:mm:ss' (UTC)'").withZone(DateTimeZone.UTC)
            subject.text + " -- " + post.createdTime.map(_.toString(timeFormatter))
          } else {
            subject.text
          }
        subject.withUpdatedText(newText)
    }
  }

  private def replaceNames(names: String, message: String): String = {
    names.split(" |,").filter(_.nonEmpty).foldLeft(message) {
      case (acc, name) =>
        acc.replaceAll(name, "***")
    }
  }

}
