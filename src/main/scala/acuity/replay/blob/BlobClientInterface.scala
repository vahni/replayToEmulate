package acuity.replay.blob

import com.azure.storage.blob.specialized.BlobInputStream

trait BlobClientInterface {
  def getAllBlobs(containerName: String, folderPath: String): List[String]

  def readBlob(containerName: String, blob: String): BlobInputStream
}
