package acuity.replay.blob

import java.io.InputStream

trait BlobClientInterface {
  /**
   * Recursively get all blobs under the folderPath
   *
   * @param containerName blob container
   * @param folderPath    blob folder path in container
   * @return list of all blob file paths
   */
  def getAllBlobs(containerName: String, folderPath: String): List[String]

  /**
   * Read a blob file
   *
   * @param containerName container blob is in
   * @param filePath      path to blob file
   * @return input stream for blob
   */
  def readBlob(containerName: String, filePath: String): InputStream

}
