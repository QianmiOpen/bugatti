package exceptions

/**
 * 唯一索引
 */
case class UniqueNameException(msg: String) extends Exception(msg) {
  def this() {
    this("")
  }
}
