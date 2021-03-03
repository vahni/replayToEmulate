package acuity.replay

import java.util.concurrent.Executors

import acuity.replay.BlobStructure.EhRecord
import acuity.replay.blob.BlobClient
import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import com.typesafe.scalalogging.LazyLogging
import navicore.data.navipath.dsl.NaviPathSyntax._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._


object Main extends App with LazyLogging {

  // load the configuration from application.conf
  val config = ConfigFactory.load()
  val acuityConfig = ConfigSource.fromConfig(config.getConfig("acuity")).loadOrThrow[Configuration.Acuity]

  val pool = Executors.newFixedThreadPool(acuityConfig.parallelism)
  implicit val blocking: ExecutionContext = ExecutionContext.fromExecutor(pool)

  logger.info(s"Starting replay, gathering dates")

  // extract the blob structure
  // this provides a list of base folders, each folder returned runs in parallel for blob discovery
  val blobFolders = BlobStructure(acuityConfig)
  logger.info(s"Found ${blobFolders.length} folders between ${acuityConfig.startDate} and ${acuityConfig.endDate}")
  logger.info(s"Discovering blobs")

  // note for the event hub capture this list is in chronological order
  val client = new BlobClient(acuityConfig.blob)
  val allBlobs = Await.result(BlobDiscovery(acuityConfig, client, blobFolders), 60.seconds).flatten
  logger.info(s"Found ${allBlobs.length} total blobs")
  implicit val actorSystem: ActorSystem = ActorSystem("blob-to-kafka")

  // TODO turn this into a kafka sink Any => ProducerMessage
  val consumer: Sink[Any, Future[Done]] = Sink.foreach {
    case p: EhRecord =>
      logger.info(s"got message ${p.Body.query[String]("$.timestamp").getOrElse("missing")}")
  }

  // slow
  //  val futures = allBlobs.map { d =>
  //    val folder = d.split('/').dropRight(1).mkString("/")
  //    implicit val cfg: BlobConfig =
  //      BlobConfig(
  //        acuityConfig.blob.account,
  //        acuityConfig.blob.key,
  //        acuityConfig.blob.container,
  //        Some(folder)
  //      )
  //    val connector: ActorRef =
  //      actorSystem.actorOf(AvroBlobConnector.props[EhRecord], folder.replace("/", "+"))
  //
  //    NaviBlob[EhRecord](connector).runWith(consumer)
  //  }
  //  Future.sequence(futures).onComplete { _ =>
  //    actorSystem.terminate()
  //  }

  val sources = allBlobs.map { d =>
    Future {
      // block in this future till this blob is done streaming, this enforces the parallelism limit
      // otherwise we download every single file as fast as we can and memory usage explodes
      // TODO if we need to enforce causality we can only do one at a time :(
      logger.info(s"downloaded $d")
      Await.result(Source.fromIterator(() => BlobStreamer(acuityConfig, client, d)).runWith(consumer), 5.minutes)
    }
  }

  logger.info(s"Got ${sources.length} sources")

  Future.sequence(sources).onComplete { _ =>
    actorSystem.terminate()
    pool.shutdown()
  }
}
