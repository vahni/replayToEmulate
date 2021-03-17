package acuity.replay

import acuity.replay.BlobRecords.EhRecord
import acuity.replay.blob.BlobClientInterface
import com.sksamuel.avro4s.{AvroOutputStream, AvroSchema}
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.ConfigSource
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, InputStream}
import pureconfig.generic.auto._

class BlobStreamerSpec extends AnyFlatSpec {

  private var blobResults: Array[Byte] = Array.empty

  private val config = ConfigFactory.load()
  private val acuityConfig = ConfigSource.fromConfig(config.getConfig("acuity")).loadOrThrow[Configuration.Acuity]

  class TestClient extends BlobClientInterface {
    override def getAllBlobs(containerName: String, folderPath: String): List[String] = ???

    override def readBlob(containerName: String, filePath: String): InputStream = {
      new ByteArrayInputStream(blobResults)
    }
  }
  val record = EhRecord(1234,
    "Offset",
    "EnqueuedTimeUtc",
    Map.empty,
    Map.empty,
    """{"test": "test"}""")
  it should "properly deserialize blob file" in {

    val out = new ByteArrayOutputStream()
    val os = AvroOutputStream.data[EhRecord].to(out).build()
    os.write(Seq(record))
    os.flush()
    os.close()
    blobResults = out.toByteArray

    val results = BlobStreamer(acuityConfig, new TestClient, "test").toList
    assert(results.length == 1)
    assert(results.head == record)
  }
  it should "properly deserialize blob file with multiple records" in {

    val out = new ByteArrayOutputStream()
    val os = AvroOutputStream.data[EhRecord].to(out).build()
    os.write(Seq(record, record.copy(SequenceNumber = 5678)))
    os.flush()
    os.close()
    blobResults = out.toByteArray

    val results = BlobStreamer(acuityConfig, new TestClient, "test").toList
    assert(results.length == 2)
    assert(results.head == record)
    assert(results(1) == record.copy(SequenceNumber = 5678))
  }
}