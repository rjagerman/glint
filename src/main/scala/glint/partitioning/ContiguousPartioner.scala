package glint.partitioning

import glint.models.PartialModelRef

/**
 * A uniform key partitioner with fixed number of servers and keys
 *
 * @param servers The number of servers
 * @param keys The number of keys
 */
class ContiguousPartioner(val servers: Array[PartialModelRef],
                          val keys: Long) extends Partitioner[Long] {
  override def partition(key: Long): PartialModelRef = {
    servers(Math.floor( (key.toDouble / keys.toDouble) * servers.length.toDouble ).toInt)
  }
}
