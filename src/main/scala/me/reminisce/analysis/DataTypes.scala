package me.reminisce.analysis

import me.reminisce.collections.MapExtension._
import me.reminisce.gameboard.board.GameboardEntities._
import me.reminisce.server.jsonserializer.NamedClassSerialization.NamedCaseClass

/**
  * Defines the data types necessary to generate the user summaries.
  */
object DataTypes {

  sealed abstract class DataType(id: String) extends NamedCaseClass {
    override val name: String = id
  }

  // Post only
  case object PostGeolocation extends DataType("PostGeolocation")

  case object PostCommentsNumber extends DataType("PostCommentsNumber")

  case object PostReactionNumber extends DataType("PostReactionNumber")

  case object PostReactionsOrder extends DataType("PostReactionsOrder")

  // Reactions
  abstract class ReactionType(id: String) extends DataType(id)

  case object PostWhoReacted extends ReactionType("PostWhoReacted")

  case object PostWhoCommented extends ReactionType("PostWhoCommented")

  case object PostWhoLiked extends ReactionType("PostWhoLiked")

  case object PostWhoWowed extends ReactionType("PostWhoWowed")

  case object PostWhoLaughed extends ReactionType("PostWhoLaughed")

  case object PostWhoLoved extends ReactionType("PostWhoLoved")

  case object PostWhoGotSad extends ReactionType("PostWhoGotSad")

  case object PostWhoGotAngry extends ReactionType("PostWhoGotAngry")

  // Page Only
  case object PageWhichLiked extends DataType("PageWhichLiked")

  case object PageLikeNumber extends DataType("PageLikeNumber")

  // Friends
  case object FriendWhoIsYours extends DataType("FriendWhoIsYours")

  // Both
  case object Time extends DataType("Time")

  sealed abstract class ItemType(id: String) {
    val name: String = id
  }

  case object PostType extends ItemType("Post")

  case object PageType extends ItemType("Page")

  case object FriendType extends ItemType("Item")

  def strToItemType(typeName: String): ItemType = typeName match {
    case PostType.name => PostType
    case PageType.name => PageType
    case FriendType.name => FriendType
  }

  val kindToTypes = Map[QuestionKind, List[DataType]](
    MultipleChoice ->
      List(PostWhoReacted, PostWhoCommented, PageWhichLiked, PostWhoLiked, PostWhoWowed, PostWhoLaughed, PostWhoLoved,
        PostWhoGotSad, PostWhoGotAngry, FriendWhoIsYours),
    Timeline ->
      List(Time),
    Geolocation ->
      List(PostGeolocation),
    Order ->
      List(PostReactionsOrder, PageLikeNumber)
  )

  val typeToKinds = kindToTypes.reverse

  val unusedTypes = List(PostCommentsNumber, PostReactionNumber)

  /**
    * Converts a string naming a data type to a DataType object
    *
    * @param typeName type name as a string
    * @return a DataType object
    */
  def stringToType(typeName: String): DataType = typeName match {
    case PostGeolocation.name => PostGeolocation
    case PostWhoCommented.name => PostWhoCommented
    case PostWhoReacted.name => PostWhoReacted
    case PostCommentsNumber.name => PostCommentsNumber
    case PageWhichLiked.name => PageWhichLiked
    case PostReactionNumber.name => PostReactionNumber
    case PostWhoLiked.name => PostWhoLiked
    case PostWhoWowed.name => PostWhoWowed
    case PostWhoLaughed.name => PostWhoLaughed
    case PostWhoLoved.name => PostWhoLoved
    case PostWhoGotSad.name => PostWhoGotSad
    case PostWhoGotAngry.name => PostWhoGotAngry
    case PageLikeNumber.name => PageLikeNumber
    case FriendWhoIsYours.name => FriendWhoIsYours
    case Time.name => Time
    case PostReactionsOrder.name => PostReactionsOrder
  }

  def possibleReactions = Set[ReactionType](
    PostWhoReacted, PostWhoCommented, PostWhoLiked, PostWhoWowed, PostWhoLaughed, PostWhoLoved, PostWhoGotSad, PostWhoGotAngry
  )
}
