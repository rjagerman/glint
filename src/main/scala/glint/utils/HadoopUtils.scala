package glint.utils

import java.io.{DataOutputStream, PrintWriter}
import java.net.URI

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path


/**
  *
  *
  */
object HadoopUtils {

  /**
    * Write data into DataOutputStream
    *
    * @param stream Data output stream
    * @param op     operations on this stream
    * @return true if write operations are ok
    */
  def writeHDFS(stream: DataOutputStream)(op: java.io.Writer => Boolean): Boolean = {
    val printer = new PrintWriter(stream)
    try {
      op(printer)
    } catch {
      case err: Throwable =>
        throw err
        false
    } finally {
      printer.close()
    }
  }

  /**
    *
    * @param path
    * @param user
    * @param index
    * @return
    */
  def getStream(path: String, user: String, index: Int): DataOutputStream = {
    FileSystem
      .get(new URI(path), new Configuration(), user)
      .create(new Path(path + "/part-" + (index + 1).toString))
  }
}
