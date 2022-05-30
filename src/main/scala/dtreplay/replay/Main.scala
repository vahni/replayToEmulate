package acuity.replay

import java.util.concurrent.Executors

import acuity.replay.blob.BlobClient
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
  val acuityConfig = ConfigSource.fromConfig(config.getConfig("acuity")).loadOrThrow[Configuration.Acuity]

  val pool = Executors.newFixedThreadPool(acuityConfig.parallelism)
  implicit val blocking: ExecutionContext = ExecutionContext.fromExecutor(pool)

  logger.info(s"Starting replay with ${acuityConfig.parallelism} workers, gathering dates")

  // extract the blob structure
  // this provides a list of base folders, each folder returned runs in parallel for blob discovery
  val blobFolders = BlobStructure(acuityConfig)
  logger.info(s"Found ${blobFolders.length} folders between ${acuityConfig.startDate} and ${acuityConfig.endDate}")
  logger.info(s"Discovering blobs")

  // note for the event hub capture this list is in chronological order
  val client = new BlobClient(acuityConfig.blob)
  val allBlobs = Await.result(BlobDiscovery(acuityConfig, client, blobFolders), acuityConfig.blob.listTimeout).flatten
  logger.info(s"Found ${allBlobs.length} total blobs")
  implicit val actorSystem: ActorSystem = ActorSystem("blob-to-kafka")

  val settings = ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
  val sources = allBlobs.map { d =>
    Future {
      // block in this future till this blob is done streaming, this enforces the parallelism limit
      // otherwise we download every single file as fast as we can and memory usage explodes
      // TODO if we need to enforce causality we can only do one at a time :(
      logger.info(s"downloaded $d")
      Await.result(Source.fromIterator(() => BlobStreamer(acuityConfig, client, d))
        .map(_.toKafka(acuityConfig)).filter(_.isDefined).map(_.get)
        .throttle(acuityConfig.kafka.throttle.messages, acuityConfig.kafka.throttle.duration)
        .runWith(Producer.plainSink(settings)), acuityConfig.streamTimeout)
    }
  }

  logger.info(s"Got ${sources.length} sources")

  Future.sequence(sources).onComplete { _ =>
    actorSystem.terminate()
    pool.shutdown()
  }
}
