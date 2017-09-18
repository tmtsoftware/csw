package csw.param.pb

import csw.param.models.{ArrayData, MatrixData}
import csw_params.parameter.PbParameter.Items
import csw_params.parameter_types._

abstract class ItemsFactory[T] {
  def make(xs: Seq[T]): Items
}

object ItemsFactory {
  def apply[T](implicit x: ItemsFactory[T]): ItemsFactory[T] = x

  def apply[T, I <: ItemType[T]: ItemTypeCompanion](makeItems: I ⇒ Items): ItemsFactory[T] = { xs =>
    makeItems(ItemTypeCompanion.make(xs))
  }

  implicit val ints: ItemsFactory[Int]             = ItemsFactory(Items.IntItems)
  implicit val intss: ItemsFactory[ArrayData[Int]] = ItemsFactory(Items.IntArrayItems)
}

object A extends App {
  def m[T: ItemsFactory](items: Seq[T]): Items = ItemsFactory[T].make(items)

  println(m(Seq(1, 2, 3)))
  println(ItemTypeCompanion.make[Int, IntItems](Seq(1, 2, 3)))
}
