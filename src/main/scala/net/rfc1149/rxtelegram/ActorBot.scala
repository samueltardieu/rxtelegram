package net.rfc1149.rxtelegram

import akka.NotUsed
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorSystem, Stash}
import akka.pattern.pipe
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer, ThrottleMode}
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.rfc1149.rxtelegram.Bot.{ActionAnswerInlineQuery, Command}
import net.rfc1149.rxtelegram.model._
import net.rfc1149.rxtelegram.model.media.Media

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

abstract class ActorBot(val token: String, val config: Config = ConfigFactory.load()) extends Actor with ActorLogging with Stash with Bot {

  import ActorBot._

  implicit val actorSystem: ActorSystem = context.system
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val fm: Materializer = ActorMaterializer()

  private[this] val httpErrorRetryDelay = config.as[FiniteDuration]("rxtelegram.http-error-retry-delay")

  protected[this] var me: User = _

  protected[this] def handleMessage(message: Message): Unit

  protected[this] def handleInlineQuery(inlineQuery: InlineQuery): Unit = sys.error("unhandled inline query")

  protected[this] def handleChosenInlineResult(chosenInlineResult: ChosenInlineResult): Unit = sys.error("unhandled chosen inline result")

  protected[this] def handleOther(other: Any): Unit = {
    log.info(s"received unknown content: $other")
  }

  override def preStart() =
    self ! GetMyself

  override def receive = {
    case GetMyself =>
      getMe.pipeTo(self)

    case user: User =>
      me = user
      setWebhook("")
      unstashAll()
      context.become(receiveIKnowMe)
      // There is no backpressure here so we have to throttle manually
      UpdateSource(token, config).throttle(10, 1.second, 20, ThrottleMode.Shaping).runWith(Sink.actorRef(self, NotUsed))

    case Failure(t) =>
      log.error(t, "error when getting information about myself")
      context.system.scheduler.scheduleOnce(httpErrorRetryDelay, self, GetMyself)

    case other =>
      stash()
  }

  private def tryHandle[T](kind: String, objOpt: Option[T], handle: T => Unit) = {
    objOpt.foreach { obj =>
      try {
        handle(obj)
      } catch {
        case t: Throwable =>
          log.error(t, s"exception when handling $kind $obj")
      }
    }
  }

  def receiveIKnowMe: Receive = {
    case GetMe =>
      sender ! me

    case update: Update =>
      tryHandle("message", update.message, handleMessage)
      tryHandle("inline query", update.inline_query, handleInlineQuery)
      tryHandle("chosen inline result", update.chosen_inline_result, handleChosenInlineResult)

    case data: ActionAnswerInlineQuery =>
      send(data) pipeTo sender()

    case data: Command =>
      sendToMessage(data) pipeTo sender()

    case SetWebhook(uri, certificate) =>
      setWebhook(uri, certificate).pipeTo(sender())

    case GetUserProfilePhotos(user, offset, limit) =>
      getUserProfilePhotos(user.id, offset, limit).pipeTo(sender())

    case GetFile(file_id) =>
      getFile(file_id).pipeTo(sender())

    case other =>
      try {
        handleOther(other)
      } catch {
        case t: Throwable =>
          log.error(t, s"error when handling $other")
      }
  }

}

object ActorBot {

  case object GetMe
  private[ActorBot] case object GetMyself

  case class SetWebhook(uri: String = "", certificate: Option[Media] = None)

  case class GetUserProfilePhotos(user: User, offset: Long = 0, limit: Long = 100)

  // Answer: (File, Option[ResponseEntity])
  case class GetFile(file_id: String)

}
