package glint.messages.server.request

import org.apache.hadoop.conf.Configuration
/**
  * Save Weight in HDFS
 *
  * @param path HDFS file path
  */
private[glint] case class Save(path: String, hadoopConf: Configuration = new Configuration()) extends Request
