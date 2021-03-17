package acuity.replay

import acuity.replay.blob.BlobClientInterface
import com.sksamuel.avro4s.{AvroInputStream, AvroSchema}
import scala.reflect.runtime.universe

object BlobStreamer {

  /**
   * Load the configured streaming object
   *
   * @param acuityConfig configuration object
   * @param client       blob client for Azure API
   * @param blob         input blob file
   * @return iterator to blob file contents
   */
  def apply(acuityConfig: Configuration.Acuity, client: BlobClientInterface, blob: String): Iterator[BlobRecords.BlobRecord] = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    runtimeMirror.reflectModule(runtimeMirror.staticModule(acuityConfig.blob.streamer))
      .instance.asInstanceOf[BlobStreamer.StreamerInterface](acuityConfig, client, blob)
  }

  /**
   * Stream interface
   *
   * A new streaming format must extend this interface and implement a method that returns and iterator
   */
  trait StreamerInterface {

    def apply(acuityConfig: Configuration.Acuity, client: BlobClientInterface, blob: String): Iterator[BlobRecords.BlobRecord]
  }

  /**
   * Streams blob files that are stored in Avro format by an event hub capture
   *
   */
  object EventHubStreamer extends StreamerInterface {
    def apply(acuityConfig: Configuration.Acuity, client: BlobClientInterface, blob: String): Iterator[BlobRecords.BlobRecord] = {
      val is: AvroInputStream[BlobRecords.EhRecord] = AvroInputStream.data[BlobRecords.EhRecord].from(
        client.readBlob(acuityConfig.blob.container, blob)
      ).build(AvroSchema[BlobRecords.EhRecord])
      is.iterator
    }
  }

}