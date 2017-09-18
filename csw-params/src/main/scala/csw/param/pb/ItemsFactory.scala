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

  implicit val IntItemsFactory: ItemsFactory[Int]                       = ItemsFactory(Items.IntItems)
  implicit val IntArrayItemsFactory: ItemsFactory[ArrayData[Int]]       = ItemsFactory(Items.IntArrayItems)
  implicit val IntMatrixItemsFactory: ItemsFactory[MatrixData[Int]]     = ItemsFactory(Items.IntMatrixItems)
  implicit val FloatMatrixItemsFactory: ItemsFactory[MatrixData[Float]] = ItemsFactory(Items.FloatMatrixItems)
}

object A extends App {
  def m[T: ItemsFactory](items: Seq[T]): Items = ItemsFactory[T].make(items)

  println(m(Seq(1, 2, 3)))
  println(ItemTypeCompanion.make[Int, IntItems](Seq(1, 2, 3)))
}
