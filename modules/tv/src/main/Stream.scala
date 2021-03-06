package lila.tv

import play.api.libs.json._
import StreamerList.Streamer

case class StreamOnAir(
    streamer: Streamer,
    name: String,
    url: String,
    streamId: String
) {

  def id = streamer.id

  def is(s: Streamer) = id == s.id

  def highlight = !Set("ornicar", "ornicar2")(streamer.streamerName)
}

case class StreamsOnAir(streams: List[StreamOnAir])

object Twitch {
  case class Channel(url: Option[String], status: Option[String], name: String, display_name: String)
  case class Stream(channel: Channel)
  case class Result(streams: Option[List[Stream]]) {
    def streamsOnAir(streamers: List[Streamer]) =
      ~streams map (_.channel) flatMap { c =>
        (c.url, c.status, StreamerList.findTwitch(streamers)(c.display_name)) match {
          case (Some(url), Some(status), Some(streamer)) => Some(StreamOnAir(
            name = status,
            streamer = streamer,
            url = url,
            streamId = c.name
          ))
          case _ => None
        }
      }
  }
  object Reads {
    implicit val twitchChannelReads = Json.reads[Channel]
    implicit val twitchStreamReads = Json.reads[Stream]
    implicit val twitchResultReads = Json.reads[Result]
  }
}

object Youtube {
  case class Snippet(title: String, channelId: String, liveBroadcastContent: String)
  case class Id(videoId: String)
  case class Item(id: Id, snippet: Snippet)
  case class Result(items: List[Item]) {
    def streamsOnAir(streamers: List[Streamer]) = items.flatMap { item =>
      for {
        streamer <- StreamerList.findYoutube(streamers)(item.snippet.channelId)
        if item.snippet.liveBroadcastContent == "live"
      } yield StreamOnAir(
        streamer = streamer,
        name = item.snippet.title,
        url = s"https://www.youtube.com/channel/${item.snippet.channelId}/live",
        streamId = item.id.videoId
      )
    }
  }
  object Reads {
    implicit val youtubeSnippetReads = Json.reads[Snippet]
    implicit val youtubeIdReads = Json.reads[Id]
    implicit val youtubeItemReads = Json.reads[Item]
    implicit val youtubeResultReads = Json.reads[Result]
  }
}
