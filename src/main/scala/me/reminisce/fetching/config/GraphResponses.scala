package me.reminisce.fetching.config

import com.github.nscala_time.time.Imports._
import me.reminisce.server.domain.RestMessage
import org.json4s.DefaultFormats

/**
  * Defines classes to hold the information parsed from the JSONs returned by Facebook when fetching data.
  */
object GraphResponses {
  implicit val formats = DefaultFormats

  abstract class BaseResponse()

  case class Root[T](data: Option[T], paging: Option[Paging], summary: Option[Summary] = None)

  case class Paging(previous: Option[String], next: Option[String], cursors: Option[Cursors])

  case class Cursors(before: Option[String], after: Option[String])

  case class Post(id: String,
                  from: Option[From],
                  message: Option[String],
                  story: Option[String],
                  place: Option[Place],
                  full_picture: Option[String] = None,
                  reactions: Option[Root[List[Reaction]]],
                  `type`: Option[String],
                  link: Option[String],
                  created_time: Option[String],
                  attachments: Option[Root[List[Attachment]]],
                  comments: Option[Root[List[Comment]]]
                 )

  case class PostsList(posts: List[Post]) extends RestMessage

  case class From(id: String, name: String)

  case class Attachment(description: Option[String] = None, media: Option[Media] = None, `type`: Option[String] = None)

  case class Media(image: Option[AttachmentImage])

  case class AttachmentImage(height: Int, width: Int, src: String)

  case class Tag(id: Option[String], name: Option[String], created_time: Option[DateTime], x: Option[Double], y: Option[Double])

  case class Image(height: Int, width: Int, source: String)

  case class Friend(id: String, name: String)

  case class Reaction(id: String, name: String, `type`: String)

  case class Summary(total_count: Int)

  case class Comment(id: String, from: From, like_count: Int, message: String, attachments: Option[Attachment])

  case class Photo(id: String, source: Option[String], created_time: Option[String], tags: Option[Root[List[Tag]]])

  case class Page(id: String, name: Option[String], photos: Option[Root[Photo]], fan_count: Option[Int], created_time: String)

  case class Place(id: Option[String], name: Option[String], location: Option[Location], created_time: Option[String])

  case class Location(city: Option[String],
                      country: Option[String],
                      latitude: Option[Double],
                      longitude: Option[Double],
                      street: Option[String],
                      zip: Option[String])

  case class AccessTokenResponse(access_token: String, token_type: String, expires_in: Option[String])

}
