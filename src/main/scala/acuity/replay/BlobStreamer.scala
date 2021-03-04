package acuity.replay

import acuity.replay.BlobStructure.EhRecord
import acuity.replay.blob.BlobClientInterface
import com.sksamuel.avro4s.{AvroInputStream, AvroSchema}
import scala.reflect.runtime.universe

object BlobStreamer {

  def apply(acuityConfig: Configuration.Acuity, client: BlobClientInterface, blob: String): Iterator[Any] = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    runtimeMirror.reflectModule(runtimeMirror.staticModule(acuityConfig.blob.streamer))
      .instance.asInstanceOf[BlobStreamer.StreamerInterface](acuityConfig, client, blob)
  }

  trait StreamerInterface {

    def apply(acuityConfig: Configuration.Acuity, client: BlobClientInterface, blob: String): Iterator[Any]
  }

  object EventHubCapture extends StreamerInterface {
    def apply(acuityConfig: Configuration.Acuity, client: BlobClientInterface, blob: String): Iterator[BlobStructure.EhRecord] = {
      val is: AvroInputStream[EhRecord] = AvroInputStream.data[EhRecord].from(
        client.readBlob(acuityConfig.blob.container, blob)
      ).build(AvroSchema[EhRecord])
      is.iterator
    }
  }

}