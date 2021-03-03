package acuity.replay

import acuity.replay.blob.BlobClientInterface
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.{ExecutionContext, Future}


object BlobDiscovery extends LazyLogging {

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