package ru.yudnikov.uniquer

import java.io.File

import akka.actor.{ActorSystem, Props}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import ru.yudnikov.uniquer.actors.{Router, Spammer}

object Example extends App {

  val dataDir = new File("data")
  if (!dataDir.exists()) dataDir.mkdir()

  val appName = "uniquer"
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
  actorSystem.scheduler.scheduleOnce(1.minutes, r)

}
