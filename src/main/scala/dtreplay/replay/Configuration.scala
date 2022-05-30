package bdtlab.replay

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * See application.conf for comments
 */
object Configuration {

  case class Blob(
                   account: String,
                   key: String,
                   container: String,
                   rootFolder: String,
                   structure: String,
                   streamer: String,
                   listTimeout: Duration
                 )

  case class Throttle(
                       messages: Int,
                       duration: FiniteDuration
                     )

  case class Kafka(
                    outputTopic: String,
                    keyPath: String,
                    throttle: Throttle
                  )

  case class Bdtlab(
                     blob: Blob,
                     kafka: Kafka,
                     startDate: String,
                     endDate: String,
                     parallelism: Int,
                     partitions: Int,
                     streamTimeout: Duration,
                   )

}