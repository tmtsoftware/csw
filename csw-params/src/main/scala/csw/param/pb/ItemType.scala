package csw.param.pb

trait ItemType[T] {
  def values: Seq[T]
}
