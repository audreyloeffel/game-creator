package me.reminisce.crawler.common

import com.github.nscala_time.time.Imports._
import me.reminisce.server.domain.RestMessage
import org.json4s.DefaultFormats

/**
 * Created by roger on 05/03/15.
 */
object GraphResponses {
  implicit val formats = DefaultFormats

  abstract class BaseResponse()

  case class Root[T](data: Option[T], paging: Option[Paging], summary: Option[Summary] = None)

  case class Paging(previous: Option[String], next: Option[String])

  case class Post(id: String,
                  from: Option[From],
                  message: Option[String],
                  story: Option[String],
                  place: Option[Place],
                  likes: Option[Root[List[Like]]],
                  `type`: Option[String],
                  created_time: Option[String],
                  attachments: Option[Root[List[Attachment]]],
                  comments: Option[Root[List[Comment]]]
                   )

  case class PostsList(posts: List[Post]) extends RestMessage

  case class From(id: String, name: String)

  case class Attachment(description: Option[String] = None, media: Option[Media] = None, `type`: Option[String] = None)

  case class Media(image: Option[AttachmentImage])

  case class AttachmentImage(height: Int, width: Int, src: String)

  case class UnloggedFaceBookUser(id: String, first_name: String, gender: String,
                                  last_name: String, link: String, locale: String,
                                  name: String, updated_time: String)

  case class Avatar(url: String, width: Option[Double], height: Option[Double], is_silhouette: Boolean)

  case class Pictures(picture: List[Picture])

  //Picture needs to be optional as FB still returns picture less entities
  //Even when searched for only with pictures
  case class Picture(id: String,
                     picture: Option[String],
                     created_time: String,
                     likes: Root[List[Like]],
                     comments: Root[List[Comment]],
                     from: Option[From],
                     images: Option[List[Image]],
                     name: Option[String],
                     tags: Option[List[Tag]])

  case class Tag(id: Option[String], name: Option[String], created_time: Option[DateTime], x: Option[Double], y: Option[Double])

  case class Image(height: Int, width: Int, source: String)

  case class Friend(id: String, name: String, picture: Option[Root[Avatar]])

  case class Like(id: String, name: String)

  case class Summary(total_count: Int)

  case class Comment(id: String, from: From, like_count: Int, message: String, attachments: Option[Attachment])

  case class Photo(id: String, source: Option[String], created_time: Option[String], tags: Option[Root[List[Tag]]])

  case class Page(id: String, name: Option[String], photos: Option[Root[Photo]])

  case class Place(id: Option[String], name: Option[String], location: Option[Location], created_time: Option[String])

  case class Location(city: Option[String],
                      country: Option[String],
                      latitude: Option[Double],
                      longitude: Option[Double],
                      street: Option[String],
                      zip: Option[String])


  case class Test(message: String)

}