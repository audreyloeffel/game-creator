package me.reminisce.gameboard.board

import me.reminisce.database.MongoDBEntities.{FBFrom, FBLocation}
import me.reminisce.server.domain.RestMessage
import me.reminisce.server.jsonserializer.NamedClassSerialization.NamedCaseClass
import me.reminisce.analysis.DataTypes.ReactionType
/**
  * Defines the data types used to represent a game board.
  */
object GameboardEntities {

  /**
    * Specific question type (combination of question kind and data type). Used only for display purposes.
    */
  sealed class SpecificQuestionType(id: String) extends NamedCaseClass {
    override val name: String = id
  }

  case object TLWhenDidYouShareThisPost extends SpecificQuestionType("TLWhenDidYouShareThisPost")
  case object TLWhenDidYouLikeThisPage extends SpecificQuestionType("TLWhenDidYouLikeThisPage")

  case object GeoWhatCoordinatesWereYouAt extends SpecificQuestionType("GeoWhatCoordinatesWereYouAt")

  case object MCWhoMadeThisCommentOnYourPost extends SpecificQuestionType("MCWhoMadeThisCommentOnYourPost")
  case object MCWhichPageDidYouLike extends SpecificQuestionType("MCWhichPageDidYouLike")
  case object MCWhoReactedToYourPost extends SpecificQuestionType("MCWhoReactedToYourPost")
  case object MCWhoReactedToYourPostWithLIKE extends SpecificQuestionType("MCWhoReactedToYourPostWithLIKE")
  case object MCWhoReactedToYourPostWithWOW extends SpecificQuestionType("MCWhoReactedToYourPostWithWOW")
  case object MCWhoReactedToYourPostWithHAHA extends SpecificQuestionType("MCWhoReactedToYourPostWithHAHA")
  case object MCWhoReactedToYourPostWithLOVE extends SpecificQuestionType("MCWhoReactedToYourPostWithLOVE")
  case object MCWhoReactedToYourPostWithSAD extends SpecificQuestionType("MCWhoReactedToYourPostWithSAD")
  case object MCWhoReactedToYourPostWithANGRY extends SpecificQuestionType("MCWhoReactedToYourPostWithANGRY")
  case object MCWhoIsYourFriend extends SpecificQuestionType("MCWhoIsYourFriend")

  case object ORDPageLikes extends SpecificQuestionType("ORDPageLikes")
  case object ORDPostCommentsNumber extends SpecificQuestionType("ORDPostCommentsNumber")
  case object ORDPostLikesNumber extends SpecificQuestionType("ORDPostLikesNumber")
  case object ORDPostTime extends SpecificQuestionType("ORDPostTime")
  case object ORDPageLikeTime extends SpecificQuestionType("ORDPageLikeTime")
  case object ORDPostReactionsNumber extends SpecificQuestionType("ORDPostReactionsNumber")

  def strToType(string: String): SpecificQuestionType = string match {
    case TLWhenDidYouShareThisPost.name => TLWhenDidYouShareThisPost
    case TLWhenDidYouLikeThisPage.name => TLWhenDidYouLikeThisPage
    case GeoWhatCoordinatesWereYouAt.name => GeoWhatCoordinatesWereYouAt
    case MCWhoMadeThisCommentOnYourPost.name => MCWhoMadeThisCommentOnYourPost
    case MCWhichPageDidYouLike.name => MCWhichPageDidYouLike
    case MCWhoReactedToYourPost.name => MCWhoReactedToYourPost
    case MCWhoReactedToYourPostWithLIKE.name => MCWhoReactedToYourPostWithLIKE
    case MCWhoReactedToYourPostWithWOW.name => MCWhoReactedToYourPostWithWOW
    case MCWhoReactedToYourPostWithHAHA.name => MCWhoReactedToYourPostWithHAHA
    case MCWhoReactedToYourPostWithLOVE.name => MCWhoReactedToYourPostWithLOVE
    case MCWhoReactedToYourPostWithSAD.name => MCWhoReactedToYourPostWithSAD
    case MCWhoReactedToYourPostWithANGRY.name => MCWhoReactedToYourPostWithANGRY
    case ORDPageLikes.name => ORDPageLikes
    case ORDPageLikeTime.name => ORDPageLikeTime
    case ORDPostReactionsNumber.name => ORDPostReactionsNumber
    case MCWhoIsYourFriend.name => MCWhoIsYourFriend
  }

  sealed abstract class QuestionKind(id: String) extends NamedCaseClass {
    override val name: String = id
  }

  case object MultipleChoice extends QuestionKind("MultipleChoice")
  case object Timeline extends QuestionKind("Timeline")
  case object Geolocation extends QuestionKind("Geolocation")
  case object Order extends QuestionKind("Order")
  case object Misc extends QuestionKind("Misc")

  def strToKind(string: String): QuestionKind = string match {
    case MultipleChoice.name => MultipleChoice
    case Timeline.name => Timeline
    case Geolocation.name => Geolocation
    case Order.name => Order
    case Misc.name => Misc
  }

  /**
    * Time units used in Timeline questions
    */
  sealed class TimeUnit(id: String) extends NamedCaseClass {
    override val name: String = id
  }

  case object Day extends TimeUnit("Day")
  case object Week extends TimeUnit("Week")
  case object Month extends TimeUnit("Month")
  case object Year extends TimeUnit("Year")

  def strToTimeUnit(string: String): TimeUnit = string match {
    case Day.name => Day
    case Week.name => Week
    case Month.name => Month
    case Year.name => Year
  }

  sealed class SubjectType(id: String) extends NamedCaseClass {
    override val name: String = id
  }

  case object PageSubject extends SubjectType("Page")
  case object TextPost extends SubjectType("TextPost")
  case object ImagePost extends SubjectType("ImagePost")
  case object VideoPost extends SubjectType("VideoPost")
  case object LinkPost extends SubjectType("LinkPost")
  case object CommentSubject extends SubjectType("Comment")
  case object Empty extends SubjectType("Empty")
  case object Reactions extends SubjectType("Reactions")

  def strToSubjectType(string: String): SubjectType = string match {
    case PageSubject.name => PageSubject
    case TextPost.name => TextPost
    case ImagePost.name => ImagePost
    case VideoPost.name => VideoPost
    case LinkPost.name => LinkPost
    case CommentSubject.name => CommentSubject
    case Empty.name => Empty
    case Reactions.name => Reactions
  }

  /**
    * Abstract subject, a subject represents a facebook item
    *
    * @param `type` type of the subject
    */
  abstract sealed class Subject(`type`: SubjectType)

  abstract sealed class PostSubject(`type`: SubjectType, val text: String, from: Option[FBFrom]) extends Subject(`type`) {
    def withUpdatedText(newText: String): PostSubject
  }

  case class PageSubject(name: String, pageId: String,
                         photoUrl: Option[String],
                         `type`: SubjectType = PageSubject) extends Subject(`type`)

  case class TextPostSubject(override val text: String, `type`: SubjectType = TextPost,
                             from: Option[FBFrom]) extends PostSubject(`type`, text, from) {
    override def withUpdatedText(newText: String): TextPostSubject = {
      this.copy(text = newText)
    }
  }

  case class ImagePostSubject(override val text: String, imageUrl: Option[String], facebookImageUrl: Option[String],
                              `type`: SubjectType = ImagePost,
                              from: Option[FBFrom]) extends PostSubject(`type`, text, from) {
    override def withUpdatedText(newText: String): ImagePostSubject = {
      this.copy(text = newText)
    }
  }

  case class VideoPostSubject(override val text: String, thumbnailUrl: Option[String], url: Option[String],
                              `type`: SubjectType = VideoPost,
                              from: Option[FBFrom]) extends PostSubject(`type`, text, from) {
    override def withUpdatedText(newText: String): VideoPostSubject = {
      this.copy(text = newText)
    }
  }

  case class LinkPostSubject(override val text: String, thumbnailUrl: Option[String], url: Option[String],
                             `type`: SubjectType = LinkPost,
                             from: Option[FBFrom]) extends PostSubject(`type`, text, from) {
    override def withUpdatedText(newText: String): LinkPostSubject = {
      this.copy(text = newText)
    }
  }

  case class CommentSubject(comment: String, post: PostSubject, `type`: SubjectType = CommentSubject) extends Subject(`type`)
  case object EmptySubject extends Subject(Empty)
  case class ReactionSubject(reactionType: ReactionType) extends Subject(Reactions)

  /**
    * Abstract game question
    *
    * @param userId  user for which the question is
    * @param kind    kind of question (See [[me.reminisce.gameboard.board.GameboardEntities.QuestionKind]]
    * @param `type`  type of question (See [[me.reminisce.gameboard.board.GameboardEntities.SpecificQuestionType]]
    * @param subject subject of the question (See [[me.reminisce.gameboard.board.GameboardEntities.Subject]]
    */
  abstract sealed class GameQuestion(userId: String, kind: QuestionKind, `type`: SpecificQuestionType, subject: Option[Subject])

  case class MultipleChoiceQuestion(userId: String,
                                    kind: QuestionKind,
                                    `type`: SpecificQuestionType,
                                    subject: Option[Subject],
                                    choices: List[Possibility],
                                    answer: Int) extends GameQuestion(userId, kind, `type`, subject)

  case class TimelineQuestion(userId: String,
                              kind: QuestionKind,
                              `type`: SpecificQuestionType,
                              subject: Option[Subject],
                              answer: String, // Weird problem with DateTime format serialization
                              min: String,
                              max: String,
                              default: String,
                              unit: TimeUnit,
                              step: Int,
                              threshold: Int) extends GameQuestion(userId, kind, `type`, subject)

  case class OrderQuestion(userId: String,
                           kind: QuestionKind,
                           `type`: SpecificQuestionType,
                           subject: Option[Subject],
                           choices: List[SubjectWithId],
                           answer: List[Int]
                          ) extends GameQuestion(userId, kind, `type`, subject)

  case class SubjectWithId(subject: Subject, uId: Int)

  case class Possibility(name: String, imageUrl: Option[String], `type`: String, fbId: Option[String] = None)

  case class GeolocationQuestion(userId: String,
                                 kind: QuestionKind,
                                 `type`: SpecificQuestionType,
                                 subject: Option[Subject],
                                 answer: FBLocation) extends GameQuestion(userId, kind, `type`, subject)

  case class Tile(`type`: QuestionKind,
                  question1: GameQuestion,
                  question2: GameQuestion,
                  question3: GameQuestion) extends RestMessage

  case class Board(userId: String, tiles: List[Tile], isTokenStale: Boolean, strategy: String) extends RestMessage

}
