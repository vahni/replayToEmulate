package acuity.replay

import java.util.concurrent.Executors

import acuity.replay.blob.BlobClientInterface
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


object BlobDiscovery extends LazyLogging {

  def apply(acuityConfig: Configuration.Acuity, client: BlobClientInterface, rootFolders: List[String]): List[String] = {
    // create an executor to run our blocking tasks in parallel
    val pool = Executors.newFixedThreadPool(acuityConfig.parallelism)
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(pool)
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
    val results: Seq[List[String]] = Await.result(resultFutures, 60.seconds)
    // note for the event hub capture this list is in chronological order
    val allBlobs = results.flatten
    pool.shutdown()
    allBlobs.toList
  }

}