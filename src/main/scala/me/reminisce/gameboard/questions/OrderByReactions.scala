package me.reminisce.gameboard.questions

import akka.actor.Props
import com.github.nscala_time.time.Imports._
import me.reminisce.database.MongoCollections
import me.reminisce.database.MongoDBEntities.FBReaction
import me.reminisce.gameboard.board.GameboardEntities.{ORDPostTime, Order, OrderQuestion, ORDPostReactionsNumber, ReactionSubject}
import me.reminisce.gameboard.questions.QuestionGenerator._
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import scala.util.Random

/**
  * Factory for [[me.reminisce.gameboard.questions.OrderByReactions]]
  */
object OrderByReactions {

  /**
    * Creates an OrderByReactions question generator
    *
    * @param database database from which to take the data
    * @return props for the created actor
    */
  def props(database: DefaultDB): Props =
  Props(new OrderByReactions(database))
}

/**
  * OrderByReactions question generator
  *
  * @param db database from which to take the data
  */
class OrderByReactions(db: DefaultDB) extends OrderQuestionGenerator { 

  def receive = {
    case CreateQuestion(userId, itemId) =>
      val client = sender
      val postsCollection = db[BSONCollection](MongoCollections.fbPosts)

      fetchPosts(postsCollection, userId, List(itemId), client) {
        postsList => {
          if(postsList.isEmpty) {
            client ! NotEnoughData("No post found.")
          } else {
            val post = postsList.head     
            val reactions = post.reactions.getOrElse(Set()).groupBy{
              case FBReaction(_, tpe) => tpe
            }
            val threeReactionsWithCounts = Random.shuffle(reactions).take(3).map{
              case (reactionType, reactionList) => (reactionType, reactionList.size)
            }
            val sortedThreeReactionsWithCounts = threeReactionsWithCounts.toList.sortBy{
              case (reactionType, reactionCount) => reactionCount
            }
            val subjects = sortedThreeReactionsWithCounts.map{case (reactionType, count) => ReactionSubject(reactionType)}           
            val question = OrderQuestionGenerator.generateSubjectsWithId(subjects) match {
              case (subjectsWithId, answer) =>
              OrderQuestion(userId, Order, ORDPostReactionsNumber, None, subjectsWithId, answer)
            }
            client ! FinishedQuestionCreation(question)
          }
        }
      }
    case any =>
      log.error(s"OrderByReactions received an unknown message : $any.")
  }
}