package me.reminisce.gameboard.questions

import akka.actor.Props
import me.reminisce.analysis.DataAnalyser
import me.reminisce.analysis.DataTypes._
import me.reminisce.database.AnalysisEntities.UserSummary
import me.reminisce.database.MongoCollections
import me.reminisce.database.MongoDBEntities.{AbstractReaction, FBPost, filterReaction, FBFriend}
import me.reminisce.gameboard.board.GameboardEntities._
import me.reminisce.gameboard.questions.QuestionGenerator._
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

object WhoIsYourFriend {
/**
    * Creates a WhoIsYourFriend question generator
    *
    * @param database database from which to take the data
    * @return props for the created actor
    */
  def props(database: DefaultDB): Props =
    Props(new WhoIsYourFriend(database))
}
/**
  * WhoIsYourFriend question generator
  *
  * @param friends set of friends
  * @param notFriends set of people that are not friends
  */

class WhoIsYourFriend(database: DefaultDB) extends QuestionGenerator {

def receive = {
  case CreateQuestion(userId, friendName) =>

    val client = sender()

    val userCollection = database[BSONCollection](MongoCollections.userSummaries)
    (for {
      maybeUserSummary <- userCollection.find(BSONDocument("userId" -> userId)).one[UserSummary]
      } 
      yield{
        val maybeQuestion = 
          for {
            userSummary <- maybeUserSummary
            friends: Set[String] = userSummary.friends.map(x => x.name)            
            notFriends: Set[String] = userSummary.reactioners.filterNot{
              case x: AbstractReaction => friends.contains(x.from.userName) || x.from.userId == userSummary.userId
            }.map{
              x => x.from.userName
            }
            if notFriends.size >= 3 
            answer = friendName
            shuffledNames = Random.shuffle((answer :: Random.shuffle(notFriends.toList).take(3)))
            choices = shuffledNames.map{
              c => Possibility(c, None, "Person", None)
            }    
          } 
          yield {
            MultipleChoiceQuestion(userId, MultipleChoice, MCWhoIsYourFriend, Some(EmptySubject), choices, shuffledNames.indexOf(answer))
          }
          maybeQuestion match {
            case Some(question) =>
              client ! FinishedQuestionCreation(question)
            case None =>
              client ! NotEnoughData(s"No user summary for $userId or not enough non friends.")
          }
        }) onFailure {
        case e =>
          client ! MongoDBError(s"${e.getMessage}")
      }
  case any => log.error(s"WhoIsYourFriend received a unexpected message $any")
}

}

