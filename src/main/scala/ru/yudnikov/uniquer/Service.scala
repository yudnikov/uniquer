package ru.yudnikov.uniquer

import akka.actor.{ActorSystem, Props}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import akka.http.scaladsl.{Http, server}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats}
import ru.yudnikov.uniquer.actors.Router
import ru.yudnikov.uniquer.actors.Router.Ask

import scala.io.StdIn
import scala.util.{Failure, Success}

object Service extends App {

  val appName = "unifier"

  implicit val actorSystem: ActorSystem = ActorSystem(appName)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  val router = actorSystem.actorOf(Props(classOf[Router], 1024))

  implicit val formats: Formats = DefaultFormats
  implicit val timeout: Timeout = 1.minute

  def route: Route = {
    get {
      pathSingleSlash {
        complete("welcome!")
      } ~
      path("stat") {
        val f = router ? Ask
        onComplete(f) {
          case Success(set: Set[String]) =>
            complete(200 -> set.mkString(";"))
          case Failure(exception) =>
            complete(500 -> exception.getMessage)
        }
      }
    } ~ post {
      pathSingleSlash {
        complete("welcome!")
      } ~
      path("user") {
        entity(as[String]) { json =>
          val maybeUsername = Serialization.read[Map[String, String]](json).get("user_id")
          maybeUsername.map { username =>
            router ! username
            complete(200 -> "")
          }.getOrElse {
            complete(500 -> "")
          }
        }
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())

  /*val appName = "uniquer"
  val actorSystem = ActorSystem(appName)
  val router = actorSystem.actorOf(Props(classOf[Router], 1024))
  val spammer = actorSystem.actorOf(Props(classOf[Spammer], router, 100000))
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  actorSystem.scheduler.scheduleOnce(10.seconds, () => {
    actorSystem.stop(spammer)
  })
  val r: Runnable = () => {
    actorSystem.actorOf(Props(classOf[Spammer], router, 100))
  }
  actorSystem.scheduler.scheduleOnce(1.minutes, r)*/

}