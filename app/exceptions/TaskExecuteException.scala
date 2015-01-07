package exceptions

/**
 * Created by jinwei on 4/1/15.
 */
case class TaskExecuteException(msg: String) extends Exception(msg: String) {
  def this() {
    this("")
  }
}
