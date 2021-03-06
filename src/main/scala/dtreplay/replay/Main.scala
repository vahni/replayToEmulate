package bdtlab.replay

import java.util.concurrent.Executors

import bdtlab.replay.blob.BlobClient
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.{Await, ExecutionContext, Future}


object Main extends App with LazyLogging {

  // load the configuration from application.conf
  val config = ConfigFactory.load()
  val bdtlabConfig = ConfigSource.fromConfig(config.getConfig("bdtlab")).loadOrThrow[Configuration.Bdtlab]

  val pool = Executors.newFixedThreadPool(bdtlabConfig.parallelism)
  implicit val blocking: ExecutionContext = ExecutionContext.fromExecutor(pool)

  logger.info(s"Starting replay with ${bdtlabConfig.parallelism} workers, gathering dates")

  // extract the blob structure
  // this provides a list of base folders, each folder returned runs in parallel for blob discovery
  val blobFolders = BlobStructure(bdtlabConfig)
  logger.info(s"Found ${blobFolders.length} folders between ${bdtlabConfig.startDate} and ${bdtlabConfig.endDate}")
  logger.info(s"Discovering blobs")

  // note for the event hub capture this list is in chronological order
  val client = new BlobClient(bdtlabConfig.blob)
  val allBlobs = Await.result(BlobDiscovery(bdtlabConfig, client, blobFolders), bdtlabConfig.blob.listTimeout).flatten
  logger.info(s"Found ${allBlobs.length} total blobs")
  implicit val actorSystem: ActorSystem = ActorSystem("blob-to-kafka")

  val settings = ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
  val sources = allBlobs.map { d =>
    Future {
      // block in this future till this blob is done streaming, this enforces the parallelism limit
      // otherwise we download every single file as fast as we can and memory usage explodes
      // TODO if we need to enforce causality we can only do one at a time :(
      logger.info(s"downloaded $d")
      Await.result(Source.fromIterator(() => BlobStreamer(bdtlabConfig, client, d))
        .map(_.toKafka(bdtlabConfig)).filter(_.isDefined).map(_.get)
        .throttle(bdtlabConfig.kafka.throttle.messages, bdtlabConfig.kafka.throttle.duration)
        .runWith(Producer.plainSink(settings)), bdtlabConfig.streamTimeout)
    }
  }

  logger.info(s"Got ${sources.length} sources")

  Future.sequence(sources).onComplete { _ =>
    actorSystem.terminate()
    pool.shutdown()
  }
}
