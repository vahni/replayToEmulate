package bdtlab.replay

import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.ProducerRecord
import bdtlab.replay.Configuration.Bdtlab
import navicore.data.navipath.dsl.NaviPathSyntax._

object BlobRecords {

  /**
   * Represents a message in file
   */
  trait BlobRecord extends LazyLogging {
    /**
     * Convert a message to a kafka record
     *
     * @param bdtlabConfig configuration object
     * @return ProducerRecord
     */
    def toKafka(bdtlabConfig: Bdtlab): Option[ProducerRecord[Array[Byte], String]]
  }

  case class EhRecord(SequenceNumber: Long,
                      Offset: String,
                      EnqueuedTimeUtc: String,
                      SystemProperties: Map[String, String],
                      Properties: Map[String, String],
                      Body: String) extends BlobRecord {

    def toKafka(bdtlabConfig: Bdtlab): Option[ProducerRecord[Array[Byte], String]] = {
      logger.debug(s"got message ${Body.query[String]("$.timestamp").getOrElse("missing")}")
      try {
        val key = Body.query[String](bdtlabConfig.kafka.keyPath).getOrElse {
          logger.warn(s"Using default key for $Body")
          Body.hashCode.toString
        }
        Some(new ProducerRecord[Array[Byte], String](bdtlabConfig.kafka.outputTopic,
          key.getBytes("UTF8"), Body))
      } catch {
        case e: Exception =>
          logger.error(s"Failed to parse ${Body} $e")
          None
      }
    }

  }

}