package csw.services.cs.internal

import java.io.File

import scala.concurrent.Future

object Annex {

  def post(file: File): Future[String] = ???

  def get(id: String, file: File): Future[File] = ???
}
