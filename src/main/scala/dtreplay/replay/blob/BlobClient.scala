package acuity.replay.blob

import java.time.Duration
import java.util.Locale
import scala.jdk.CollectionConverters._
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.ListBlobsOptions
import com.azure.storage.common.StorageSharedKeyCredential
import acuity.replay.Configuration
import java.io.InputStream


class BlobClient(blob: Configuration.Blob) extends BlobClientInterface {

  // https://github.com/Azure/azure-sdk-for-java/blob/master/sdk/storage/azure-storage-blob/src/samples/java/com/azure/storage/blob/BasicExample
  private val credential = new StorageSharedKeyCredential(blob.account, blob.key)
  private val endpoint: String = String.format(Locale.ROOT, "https://%s.blob.core.windows.net", blob.account)
  private val storageClient = new BlobServiceClientBuilder().endpoint(endpoint).credential(credential).buildClient

  def getAllBlobs(containerName: String, folderPath: String): List[String] = {
    storageClient.getBlobContainerClient(containerName)
      .listBlobs(new ListBlobsOptions().setPrefix(folderPath), Duration.ofNanos(blob.listTimeout.toNanos))
      .asScala.map(_.getName).toList
  }

  def readBlob(containerName: String, filePath: String): InputStream = {
    storageClient.getBlobContainerClient(containerName)
      .getBlobClient(filePath)
      .getBlockBlobClient
      .openInputStream
  }
}
