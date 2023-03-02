package jestfs.util

/** unique ids */
trait UId extends IntId {
  // unique ids
  def id: Int

  // get simple string
  def simpleString: String = s"${getClass.getSimpleName}[$id]"

  // override equality comparison using unique ids
  override def equals(that: Any): Boolean = that match
    case that: UId if this.id != -1 =>
      (this.getClass eq that.getClass) &&
      (this.id == that.id)
    case that: UId => this eq that
    case _         => false

  // override hashCode using unique ids
  override def hashCode: Int = id
}
given Ordering[UId] = Ordering.by(_.id)
