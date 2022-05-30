package bdtlab.replay

import bdtlab.replay.blob.BlobClientInterface
import com.sksamuel.avro4s.{AvroInputStream, AvroSchema}
import scala.reflect.runtime.universe

object BlobStreamer {

  /**
   * Load the configured streaming object
   *
   * @param bdtlabConfig configuration object
   * @param client       blob client for Azure API
   * @param blob         input blob file
   * @return iterator to blob file contents
   */
  def apply(bdtlabConfig: Configuration.Bdtlab, client: BlobClientInterface, blob: String): Iterator[BlobRecords.BlobRecord] = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    runtimeMirror.reflectModule(runtimeMirror.staticModule(bdtlabConfig.blob.streamer))
      .instance.asInstanceOf[BlobStreamer.StreamerInterface](bdtlabConfig, client, blob)
  }

  /**
   * Stream interface
   *
   * A new streaming format must extend this interface and implement a method that returns and iterator
   */
  trait StreamerInterface {

    def apply(bdtlabConfig: Configuration.Bdtlab, client: BlobClientInterface, blob: String): Iterator[BlobRecords.BlobRecord]
  }

  /**
   * Streams blob files that are stored in Avro format by an event hub capture
   *
   */
  object EventHubStreamer extends StreamerInterface {
    def apply(bdtlabConfig: Configuration.Bdtlab, client: BlobClientInterface, blob: String): Iterator[BlobRecords.BlobRecord] = {
      val is: AvroInputStream[BlobRecords.EhRecord] = AvroInputStream.data[BlobRecords.EhRecord].from(
        client.readBlob(bdtlabConfig.blob.container, blob)
      ).build(AvroSchema[BlobRecords.EhRecord])
      is.iterator
    }
  }

}