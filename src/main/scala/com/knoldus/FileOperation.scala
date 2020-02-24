package com.knoldus

import akka.routing.{RoundRobinPool}
import java.io.File
import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout

import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.Source

case class LogStatus(file: String, error: Int, warnings: Int, info: Int)

class FileOperation extends Actor with ActorLogging {
  override def receive: Receive = {
    case file: File =>
      val result = getLogs(file)
      log.info(result.toString)

  }

  def getLogs(file: File): LogStatus = {
    try {
      val fileContent = readFile(file)

      val errors = readError(fileContent)
      val warnings = readWarnings(fileContent)
      val info = readInfo(fileContent)

      LogStatus(file.getName, errors, warnings, info)
    }
  }

  def readFile(file: File): (List[String]) = {
    if (file.isFile) {
      Source.fromFile(file).getLines().toList
    }
    else {
      throw new Exception("not file")
    }
  }

  def checkFile(filesOrFolder: List[File]): List[File] = {
    filesOrFolder.filter(_.isFile)

  }

  def readError(filesContent: List[String]): Int = {
    (filesContent.count(_.contains("[ERROR]")))


  }

  def readWarnings(filesContent: List[String]): Int = {

    filesContent.count(_.contains("[WARN]"))

  }

  def readInfo(filesContent: List[String]): Int = {
    filesContent.count(_.contains("[INFO]"))

  }


}

object FileOperation extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("LogFilesActorSystem", config.getConfig("configuration"))
  val confStr="default-dispatcher"
  val threads=6
  val ref = system.actorOf(RoundRobinPool(threads).props(Props[FileOperation] withDispatcher(confStr)), "FileOperation")


  val pathObj = new File("src/main/resources/logfiles")

  val filesOrFoldersList = pathObj.listFiles().toList
  implicit val timeout = Timeout(5. seconds)
  // for (filesOrFolders <- filesOrFoldersList){
  //  ref ! filesOrFolders
   filesOrFoldersList.map(filesOrFolders => ref ! filesOrFolders)
}
