package csw.param.pb

import csw_params.parameter.PbParameter.Items
import csw_params.parameter.PbParameter.Items.IntItems
import csw_params.parameter_types

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
  def apply[T](implicit x: ItemsFactory[T]): ItemsFactory.Aux[T, x.S] = x.asInstanceOf

  implicit val IntItemsFactory: ItemsFactory.Aux[Int, parameter_types.IntItems] = ItemsFactory.from(IntItems)
}

object A {
  private val items: parameter_types.IntItems = parameter_types.IntItems().addValues(1, 2, 3)

//  implicitly[ItemsFactory[Int] { type S = parameter_types.IntItems }].make(items)
//  implicitly[ItemsFactory.Aux[Int, parameter_types.IntItems]].make(items)

  ItemsFactory[Int].make(items)
}
