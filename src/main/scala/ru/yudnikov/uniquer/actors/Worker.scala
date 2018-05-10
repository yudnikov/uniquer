package ru.yudnikov.uniquer.actors

import java.io.{File, FileOutputStream}

import akka.actor.{Actor, Cancellable}
import ru.yudnikov.uniquer.actors.Router.Stat

import scala.concurrent.ExecutionContext
import scala.io.Source

case class Worker(id: Int) extends Actor {
  implicit val ec: ExecutionContext = context.system.dispatcher
  val fileName: String = s"data/$id.txt"
  var fileOutputStream: FileOutputStream = _
  var strings: Set[String] = Set()

  override def preStart(): Unit = {
    recover()
    fileOutputStream = new FileOutputStream(fileName, true)
    super.preStart()
  }

  private def recover(): Unit = {
    println(s"recovering: $id")
    val file = new File(fileName)
    if (file.exists()) {
      strings = Source.fromFile(fileName).getLines().toSet
    }
  }

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
