package csw.param.pb

trait ItemType[T] {
  def values: Seq[T]
  def withValues(xs: Seq[T]): Any
  def set(xs: Seq[T]): this.type = withValues(xs).asInstanceOf[this.type]
}

trait ItemTypeCompanion[S] {
  def defaultInstance: S
}

object ItemTypeCompanion {
  def apply[T](implicit x: ItemTypeCompanion[T]): ItemTypeCompanion[T] = x
  def make[T, S <: ItemType[T]: ItemTypeCompanion](items: Seq[T]): S = {
    ItemTypeCompanion[S].defaultInstance.set(items)
  }
}
