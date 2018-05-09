package ru.yudnikov.uniquer.actors

import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, ActorRef, Cancellable}
import ru.yudnikov.uniquer.actors.Router.Ask
import ru.yudnikov.uniquer.actors.Spammer.Spam

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class Spammer(target: ActorRef, messagesPerSecond: Int) extends Actor {
  implicit val ec: ExecutionContext = context.system.dispatcher
  val spamTask: Cancellable = context.system.scheduler.schedule(1.seconds, 1.second, self, Spam)
  val askTask: Cancellable = context.system.scheduler.schedule(500.millis, 5.second, target, Ask)

  override def postStop(): Unit = {
    println(s"stopping $getClass")
    spamTask.cancel()
    askTask.cancel()
    super.postStop()
  }

  override def receive: Receive = {
    case Spam =>
      1 to messagesPerSecond foreach { _ =>
        target ! s"user${ThreadLocalRandom.current().nextInt()}"
      }
  }
}

object Spammer {
  case object Spam
}
