package bdtlab.replay

import java.util.concurrent.Executors

import bdtlab.replay.blob.BlobClientInterface
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
  private val bdtlabConfig = ConfigSource.fromConfig(config.getConfig("bdtlab")).loadOrThrow[Configuration.Bdtlab]
  private val pool = Executors.newFixedThreadPool(bdtlabConfig.parallelism)
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

    assert(Await.result(BlobDiscovery(bdtlabConfig.copy(blob = bdtlabConfig.blob.copy(rootFolder = "")),
      new TestClient, List("myFolder1")), bdtlabConfig.blob.listTimeout).flatten == blobResults)
  }
  it should "find all blobs under multiple folder" in {
    blobResults = List("a", "b")
    blobFolderArgs = List("test/test/myFolder1","test/test/myFolder2")

    assert(Await.result(BlobDiscovery(bdtlabConfig.copy(blob = bdtlabConfig.blob.copy(rootFolder = "test/test/")),
      new TestClient, List("myFolder1","myFolder2")), bdtlabConfig.blob.listTimeout).flatten == blobResults ++ blobResults)
  }
}