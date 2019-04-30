package csw.event.client.pb

import csw_protobuf.parameter.PbParamValues

abstract class ItemsFactory[T] {

  /**
   * Abstract method to convert a Seq[T] to Items which is a base class for all supported Items.
   * e.g. BooleanItems, StringItems etc.
   *
   * @return Items
   */
  def make(xs: Seq[T]): PbParamValues
}

object ItemsFactory {
  def apply[T](implicit x: ItemsFactory[T]): ItemsFactory[T] = x
}
