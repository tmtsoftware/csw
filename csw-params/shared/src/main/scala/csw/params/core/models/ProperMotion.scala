/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.models

case class ProperMotion(pmx: Double, pmy: Double) {
  override def toString: String = s"$pmx/$pmy"
}

object ProperMotion {
  val DEFAULT_PROPERMOTION = ProperMotion(0.0, 0.0)
}

case class PMValue(uaspyr: Long) extends AnyVal with Serializable with Ordered[PMValue] {

  // operators
  def +(a2: PMValue): PMValue = new PMValue(uaspyr + a2.uaspyr)

  def -(a2: PMValue): PMValue = new PMValue(uaspyr - a2.uaspyr)

  def *(a2: Double): PMValue = new PMValue((uaspyr * a2).toLong)

  def *(a2: Int): PMValue = new PMValue(uaspyr * a2)

  def /(a2: Double): PMValue = new PMValue((uaspyr / a2).toLong)

  def /(a2: Int): PMValue = new PMValue(uaspyr / a2)

  def unary_+ = this

  def unary_- = PMValue(-uaspyr)

  override def compare(that: PMValue): Int = uaspyr.compare(that.uaspyr)

}
