package ru.yudnikov.uniquer.actors

import java.io.{File, FileOutputStream}

import akka.actor.{Actor, Cancellable}
import ru.yudnikov.uniquer.actors.Router.Ask
import ru.yudnikov.uniquer.actors.Worker.Sleep

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.Source

case class Worker(id: Int) extends Actor {
  implicit val ec: ExecutionContext = context.system.dispatcher
  val fileName: String = s"data/$id.txt"
  var fileOutputStream: FileOutputStream = new FileOutputStream(fileName, true)
  var strings: Set[String] = Set()
  val awake: FiniteDuration = 30.seconds
  var isSleeping: Boolean = false
  var sleepTask: Cancellable = context.system.scheduler.scheduleOnce(awake, self, Sleep)
  override def preStart(): Unit = {
    wakeUp()
    super.preStart()
  }

  private def wakeUp(): Unit = {
    println(s"waking up: $id")
    val file = new File(fileName)
    if (file.exists()) {
      strings = Source.fromFile(fileName).getLines().toSet
    }
  }

  override def receive: Receive = {
    case Sleep =>
      println(s"sleeping: $id")
      strings = Set()
      isSleeping = true
    case str: String =>
      if (isSleeping) {
        wakeUp()
      }
      doNotSleep()
      if (!strings.contains(str)) {
        strings = strings + str
        fileOutputStream.write(s"$str\n".getBytes())
      }
    case Ask =>
      if (isSleeping) {
        wakeUp()
      }
      doNotSleep()
      sender() ! strings
  }

  private def doNotSleep(): Unit = {
    //println(s"$id: do not sleep!")
    sleepTask.cancel()
    sleepTask = context.system.scheduler.scheduleOnce(awake, self, Sleep)
  }
}

object Worker {
  case object Sleep
}