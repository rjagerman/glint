package glint.messages.server.request

/**
  * Save weight matrix into HDFS
  *
  * @param path HDFS file path
  * @param user HDFS user
  */
private[glint] case class Save(path: String, user:String) extends Request
