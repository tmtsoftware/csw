package csw.time.client

import com.sun.jna.Library

trait TimeLibrary extends Library {

  def clock_gettime(clock: Int, timeSpec: TimeSpec): Int
}
