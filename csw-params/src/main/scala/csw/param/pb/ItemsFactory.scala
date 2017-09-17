package csw.param.pb

import csw_params.parameter.PbParameter.Items
import csw_params.parameter_types.IntItems

class ItemsFactory[T](val make: Seq[T] ⇒ Items)

object ItemsFactory {
  def apply[T](implicit x: ItemsFactory[T]): ItemsFactory[T] = x
  implicit object IntItemsFactory extends ItemsFactory[Int](xs ⇒ Items.IntItems(IntItems().withValues(xs)))
}

object A extends App {
  def m[T: ItemsFactory](items: Seq[T]): Items = ItemsFactory[T].make(items)

  println(m(Seq(1, 2, 3)))
  println(ItemTypeCompanion.make[Int, IntItems](Seq(1, 2, 3)))
}
