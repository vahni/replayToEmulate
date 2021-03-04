package acuity.replay

object Configuration {

  case class Blob(
                   account: String,
                   key: String,
                   container: String,
                   rootFolder: String,
                   structure: String,
                   streamer: String
                 )

  case class Acuity(
                     blob: Blob,
                     startDate: String,
                     endDate: String,
                     parallelism: Int,
                     podName: String
                   )

}