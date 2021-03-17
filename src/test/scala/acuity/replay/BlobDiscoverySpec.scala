package acuity.replay

import java.util.concurrent.Executors

import acuity.replay.blob.BlobClientInterface
import com.azure.storage.blob.specialized.BlobInputStream
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import scala.concurrent.{Await, ExecutionContext}

class BlobDiscoverySpec extends AnyFlatSpec {

  private var blobResults: List[String] = List.empty
  private var blobFolderArgs: List[String] = List.empty
  private val config = ConfigFactory.load()
  private val acuityConfig = ConfigSource.fromConfig(config.getConfig("acuity")).loadOrThrow[Configuration.Acuity]
  private val pool = Executors.newFixedThreadPool(acuityConfig.parallelism)
  implicit val blocking: ExecutionContext = ExecutionContext.fromExecutor(pool)

  class TestClient extends BlobClientInterface {
    override def getAllBlobs(containerName: String, folderPath: String): List[String] = {
      assert(blobFolderArgs.contains(folderPath))
      blobResults
    }

    override def readBlob(containerName: String, filePath: String): BlobInputStream = ???
  }

  it should "find all blobs under single folder" in {
    blobResults = List("a", "b")
    blobFolderArgs = List("myFolder1")

    assert(Await.result(BlobDiscovery(acuityConfig.copy(blob = acuityConfig.blob.copy(rootFolder = "")),
      new TestClient, List("myFolder1")), acuityConfig.blob.listTimeout).flatten == blobResults)
  }
  it should "find all blobs under multiple folder" in {
    blobResults = List("a", "b")
    blobFolderArgs = List("test/test/myFolder1","test/test/myFolder2")

    assert(Await.result(BlobDiscovery(acuityConfig.copy(blob = acuityConfig.blob.copy(rootFolder = "test/test/")),
      new TestClient, List("myFolder1","myFolder2")), acuityConfig.blob.listTimeout).flatten == blobResults ++ blobResults)
  }
}