package csw.logging

package object api {

  /**
   * The type for rich messages.
   * This can be a String or Map[String,String]
   * See the project README file for other options.
   *
   */
  /**
   * Marker to indicate no exception is present
   */
  val NoLogException = new Exception("No Log Exception")
}
