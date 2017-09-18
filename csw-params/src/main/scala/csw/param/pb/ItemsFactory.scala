package csw.param.pb

import csw.param.models._
import csw_params.parameter.PbParameter.Items
import csw_params.parameter_types._

import scala.reflect.ClassTag

abstract class ItemsFactory[T] {
  def make(xs: Seq[T]): Items
}

object ItemsFactory {
  def apply[T](implicit x: ItemsFactory[T]): ItemsFactory[T] = x

  def apply[T, I <: ItemType[T]: ItemTypeCompanion](makeItems: I ⇒ Items): ItemsFactory[T] = { xs =>
    makeItems(ItemTypeCompanion.make(xs))
  }

  implicit val ChoiceItemsFactory: ItemsFactory[Choice]   = ItemsFactory(Items.ChoiceItems)
  implicit val RaDecItemsFactory: ItemsFactory[RaDec]     = ItemsFactory(Items.RaDecItems)
  implicit val StringItemsFactory: ItemsFactory[String]   = ItemsFactory(Items.StringItems)
  implicit val StructItemsFactory: ItemsFactory[Struct]   = ItemsFactory(Items.StructItems)
  implicit val BooleanItemsFactory: ItemsFactory[Boolean] = ItemsFactory(Items.BooleanItems)

  implicit val IntItemsFactory: ItemsFactory[Int]       = ItemsFactory(Items.IntItems)
  implicit val LongItemsFactory: ItemsFactory[Long]     = ItemsFactory(Items.LongItems)
  implicit val FloatItemsFactory: ItemsFactory[Float]   = ItemsFactory(Items.FloatItems)
  implicit val DoubleItemsFactory: ItemsFactory[Double] = ItemsFactory(Items.DoubleItems)

  implicit val IntArrayItemsFactory: ItemsFactory[ArrayData[Int]]       = ItemsFactory(Items.IntArrayItems)
  implicit val DoubleArrayItemsFactory: ItemsFactory[ArrayData[Double]] = ItemsFactory(Items.DoubleArrayItems)
  implicit val FloatArrayItemsFactory: ItemsFactory[ArrayData[Float]]   = ItemsFactory(Items.FloatArrayItems)
  implicit val LongArrayItemsFactory: ItemsFactory[ArrayData[Long]]     = ItemsFactory(Items.LongArrayItems)

  implicit val IntMatrixItemsFactory: ItemsFactory[MatrixData[Int]]       = ItemsFactory(Items.IntMatrixItems)
  implicit val LongMatrixItemsFactory: ItemsFactory[MatrixData[Long]]     = ItemsFactory(Items.LongMatrixItems)
  implicit val FloatMatrixItemsFactory: ItemsFactory[MatrixData[Float]]   = ItemsFactory(Items.FloatMatrixItems)
  implicit val DoubleMatrixItemsFactory: ItemsFactory[MatrixData[Double]] = ItemsFactory(Items.DoubleMatrixItems)

  implicit def notSupported[T: ClassTag]: ItemsFactory[T] =
    throw new RuntimeException(s"datatype conversion to protobuf not supported: ${scala.reflect.classTag[T]}")
}

object A extends App {
  def m[T: ItemsFactory](items: Seq[T]): Items = ItemsFactory[T].make(items)

  println(m(Seq(1, 2, 3)))
  println(ItemTypeCompanion.make[Int, IntItems](Seq(1, 2, 3)))
}
