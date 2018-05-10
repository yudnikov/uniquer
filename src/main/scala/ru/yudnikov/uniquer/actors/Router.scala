package ru.yudnikov.uniquer.actors

import java.io.File

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout
import ru.yudnikov.uniquer.actors.Router.{Stat, StatAsync}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class Router(numberWorkers: Int) extends Actor {
  implicit val ec: ExecutionContext = context.system.dispatcher

  val workers: Map[Int, ActorRef] = (0 until numberWorkers).map { i =>
    i -> context.actorOf(Props(classOf[Worker], i))
  }.toMap

  override def receive: Receive = {
    case str: String =>
      workers(scala.math.abs(str.hashCode % numberWorkers)) ! str
    case Stat =>
      val senderRef = sender()
      implicit val timeout: Timeout = 1.minute
      Future.sequence(workers.values.map(_ ? Stat)).map {
        case sets: Iterable[Set[String]] =>
          sets.par.reduceLeft(_ ++ _)
      } onComplete {
        case Success(set) =>
          senderRef ! set
        case Failure(exception) =>
          senderRef ! exception
      }
    case StatAsync =>
      val senderRef = sender()
      implicit val timeout: Timeout = 1.minute
      val futures = workers.values.map(_ ? Stat).asInstanceOf[Iterable[Future[Set[String]]]]
      Future.reduceLeft(futures.toList) { (af, bf) =>
        af | bf
      } onComplete {
        case Success(res) => senderRef ! res
      }
  }
}

object Router {

  case object Stat

  case object StatAsync

}
