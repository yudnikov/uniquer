package ru.yudnikov.uniquer.actors

import java.io.{File, FileOutputStream}

import akka.actor.{Actor, Cancellable}
import ru.yudnikov.uniquer.actors.Router.Stat

import scala.concurrent.ExecutionContext

case class Worker(id: Int) extends Actor {
  implicit val ec: ExecutionContext = context.system.dispatcher
  val fileName: String = s"data/$id.txt"
  var fileOutputStream: FileOutputStream = new FileOutputStream(fileName, true)
  var strings: Set[String] = Set()

  override def receive: Receive = {
    case str: String =>
      if (!strings.contains(str)) {
        strings = strings + str
        fileOutputStream.write(s"$str\n".getBytes())
      }
    case Stat =>
      sender() ! strings
  }
}
