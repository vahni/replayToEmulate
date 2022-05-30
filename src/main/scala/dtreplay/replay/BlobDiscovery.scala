package acuity.replay

import acuity.replay.blob.BlobClientInterface
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.{ExecutionContext, Future}


object BlobDiscovery extends LazyLogging {

  /**
   * Get a list of all blob files under a set of given blob paths.
   * The more the paths are broken up the faster this will run
   *
   * @param acuityConfig configuration object
   * @param client       blob client for Azure API
   * @param rootFolders  list of root folders to discover under
   * @param ec           implicit execution context
   * @return future to the sequence of results
   */
  def apply(acuityConfig: Configuration.Acuity, client: BlobClientInterface,
            rootFolders: List[String])(implicit ec: ExecutionContext): Future[Seq[List[String]]] = {
    // list all blobs in each base folder
    val resultFutures = Future.sequence(
      rootFolders.map(d => Future {
        val folder = s"${acuityConfig.blob.rootFolder}$d"
        logger.debug(s"Discovering blobs in $folder")
        val blobs = client.getAllBlobs(acuityConfig.blob.container, folder)
        logger.debug(s"*** Found ${blobs.length} blobs in $folder")
        blobs
      })
    )
    // note for the event hub capture this list is in chronological order
    resultFutures
  }

}