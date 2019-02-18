package csw.time

/**
 * This module provides Time Service APIs to access time in Coordinated Universal Time (UTC) and International Atomic Time (TAI) time scales with up to nano
 * second precision when available.
 *
 * In order to access current UTC or TAI Time, one could use the TMTTime API.
 *
 * {{{
 *   val utcTime: UTCTime = UTCTime.now() // gets current UTC time
 *   utcTime.toTAI // converts the given UTCTime instance to TAI timescale
 * }}}
 *
 * Similar APIs are available for TAI timescale.
 *
 * {{{
 *   val taiTime: TAITime = TAITime.now() // gets current TAI time
 *   taiTime.toUTC // converts the given TAITime instance to UTC timescale
 * }}}
 *
 */
package object core {}
