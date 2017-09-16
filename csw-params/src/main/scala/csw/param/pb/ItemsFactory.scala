package csw.param.pb

import csw_params.parameter.PbParameter.Items
import csw_params.parameter_types.IntItems

trait ItemsFactory[T] {
  type S
  def make(s: S): Items
}

object ItemsFactory {
  type Aux[T, _S] = ItemsFactory[T] { type S = _S }

  def from[T, _S <: ItemType[T]](_make: _S ⇒ Items): Aux[T, _S] = new ItemsFactory[T] {
    override type S = _S
    override def make(s: _S): Items = _make(s)
  }
  def apply[T](implicit x: ItemsFactory[T]): ItemsFactory.Aux[T, x.S] = x.asInstanceOf[ItemsFactory.Aux[T, x.S]]

  implicit val IntItemsFactory: ItemsFactory.Aux[Int, IntItems] = ItemsFactory.from(Items.IntItems)
}

object A extends App {
  def m[T: ItemsFactory, S <: ItemType[T]: ItemTypeCompanion](items: Seq[T]): Items = {
    ItemsFactory[T].make(ItemTypeCompanion[S].defaultInstance.withValues2(items))
  }

  println(m[Int, IntItems](Seq(1, 2, 3)))
}
