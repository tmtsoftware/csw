package csw.messages.params.pb

import java.time.Instant

import csw.messages.params.models._
import csw_messages_params.parameter.PbParameter.Items

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
  implicit val StructItemsFactory: ItemsFactory[Struct]   = ItemsFactory(Items.StructItems)
  implicit val InstantItemsFactory: ItemsFactory[Instant] = ItemsFactory(Items.InstantItems)

  implicit val CharItemsFactory: ItemsFactory[Char]       = ItemsFactory(Items.CharItems)
  implicit val StringItemsFactory: ItemsFactory[String]   = ItemsFactory(Items.StringItems)
  implicit val BooleanItemsFactory: ItemsFactory[Boolean] = ItemsFactory(Items.BooleanItems)

  implicit val ByteItemsFactory: ItemsFactory[Byte]     = ItemsFactory(Items.ByteItems)
  implicit val ShortItemsFactory: ItemsFactory[Short]   = ItemsFactory(Items.ShortItems)
  implicit val IntItemsFactory: ItemsFactory[Int]       = ItemsFactory(Items.IntItems)
  implicit val LongItemsFactory: ItemsFactory[Long]     = ItemsFactory(Items.LongItems)
  implicit val FloatItemsFactory: ItemsFactory[Float]   = ItemsFactory(Items.FloatItems)
  implicit val DoubleItemsFactory: ItemsFactory[Double] = ItemsFactory(Items.DoubleItems)

  implicit val ByteArrayItemsFactory: ItemsFactory[ArrayData[Byte]]     = ItemsFactory(Items.ByteArrayItems)
  implicit val ShortArrayItemsFactory: ItemsFactory[ArrayData[Short]]   = ItemsFactory(Items.ShortArrayItems)
  implicit val IntArrayItemsFactory: ItemsFactory[ArrayData[Int]]       = ItemsFactory(Items.IntArrayItems)
  implicit val DoubleArrayItemsFactory: ItemsFactory[ArrayData[Double]] = ItemsFactory(Items.DoubleArrayItems)
  implicit val FloatArrayItemsFactory: ItemsFactory[ArrayData[Float]]   = ItemsFactory(Items.FloatArrayItems)
  implicit val LongArrayItemsFactory: ItemsFactory[ArrayData[Long]]     = ItemsFactory(Items.LongArrayItems)

  implicit val ByteMatrixItemsFactory: ItemsFactory[MatrixData[Byte]]     = ItemsFactory(Items.ByteMatrixItems)
  implicit val ShortMatrixItemsFactory: ItemsFactory[MatrixData[Short]]   = ItemsFactory(Items.ShortMatrixItems)
  implicit val IntMatrixItemsFactory: ItemsFactory[MatrixData[Int]]       = ItemsFactory(Items.IntMatrixItems)
  implicit val LongMatrixItemsFactory: ItemsFactory[MatrixData[Long]]     = ItemsFactory(Items.LongMatrixItems)
  implicit val FloatMatrixItemsFactory: ItemsFactory[MatrixData[Float]]   = ItemsFactory(Items.FloatMatrixItems)
  implicit val DoubleMatrixItemsFactory: ItemsFactory[MatrixData[Double]] = ItemsFactory(Items.DoubleMatrixItems)

  implicit def genericItemsFactory[A: ItemsFactory, B](implicit conversion: A ⇒ B): ItemsFactory[B] =
    ItemsFactory[A].asInstanceOf[ItemsFactory[B]]
}
