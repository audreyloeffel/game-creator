package me.reminisce.service.gameboardgen.questiongen

import akka.actor.Props
import me.reminisce.database.MongoDatabaseService
import me.reminisce.mongodb.MongoDBEntities.{FBPage, FBPageLike}
import me.reminisce.service.gameboardgen.GameboardEntities.SpecificQuestionType._
import me.reminisce.service.gameboardgen.GameboardEntities.{MultipleChoiceQuestion, Possibility, Question}
import me.reminisce.service.gameboardgen.questiongen.QuestionGenerator.{CreateQuestion, FailedToCreateQuestion, FinishedQuestionCreation}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future
import scala.util.{Failure, Random, Success}


object WhichPageDidYouLike {
  def props(database: DefaultDB): Props =
    Props(new WhichPageDidYouLike(database))

}


class WhichPageDidYouLike(db: DefaultDB) extends QuestionGenerator {

  def receive = {
    case CreateQuestion(user_id) =>
      val client = sender()
      val pagesCollection = db[BSONCollection](MongoDatabaseService.fbPagesCollection)
      val likesCollection = db[BSONCollection](MongoDatabaseService.fbPageLikesCollection)
      val query = BSONDocument(
        "user_id" -> user_id
      )
      val likedPageIds = likesCollection.find(query).cursor[FBPageLike].collect[List]().map {
        likes =>
          likes.map {
            like =>
              like.page_id
          }
      }

      val result = likedPageIds.flatMap { pIds =>
        val queryUnliked = BSONDocument(
          "page_id" -> BSONDocument("$nin" -> pIds)
        )
        pagesCollection.find(queryUnliked).cursor[FBPage].collect[List](3).flatMap { unlikedPages =>
          if (unlikedPages.length < 3) {
            Future.failed(new Exception("Not enough unliked pages"))
          } else {
            val likedPageId = Random.shuffle(pIds).head
            val likedPage = pagesCollection.find(BSONDocument {
              "page_id" -> likedPageId
            }).one[FBPage]
            likedPage.map {
              _.map { answer =>
                val question = Question("WhichPageDidYouLike")

                val possibilities = (answer :: unlikedPages).map { c =>
                  val source = c.photos match {
                    case Some(p) => p.source
                    case None => Some("")
                  }
                  Possibility(c.name, source, Some(c.page_id))
                }.toVector

                val answerPossibility = possibilities(0)
                val randomPossibilities = Random.shuffle(possibilities)

                MultipleChoiceQuestion(answerPossibility.text.get,
                  user_id, question, randomPossibilities,
                  randomPossibilities.indexOf(answerPossibility))
              }
            }

          }
        }

      }

      result.onComplete {
        case Success(Some(question)) => client ! FinishedQuestionCreation(question)
        case Failure(e) =>
          log.error("Failed to created WhichPageDidYouLike as this user likes EVERYTHING " + e)
          client ! FailedToCreateQuestion("Unable to create question WhichPageDidYouLike", MCWhichPageDidYouLike)
        case _ =>
          log.error("Failed to created WhichPageDidYouLike as this user likes EVERYTHING ")
          client ! FailedToCreateQuestion("Unable to create question WhichPageDidYouLike", MCWhichPageDidYouLike)

      }

    case x => log.error(s"WhichPageDidYouLike received a unexpected message " + x)
  }

}