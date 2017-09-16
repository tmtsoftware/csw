package csw.param.pb

import csw_params.parameter.PbParameter.Items
import csw_params.parameter_types.IntItems

class ItemsFactory[T, S <: ItemType[T]](val make: S ⇒ Items)

object ItemsFactory {
  implicit object IntItemsFactory extends ItemsFactory[Int, IntItems](Items.IntItems)
}

object A extends App {
  def m[T, S <: ItemType[T]: ItemTypeCompanion](items: Seq[T])(implicit dd: ItemsFactory[T, S]): Items = {
    dd.make(ItemTypeCompanion[S].defaultInstance.withValues2(items))
  }

  println(m[Int, IntItems](Seq(1, 2, 3)))
  println(ItemTypeCompanion.make[Int, IntItems](Seq(1, 2, 3)))
}
