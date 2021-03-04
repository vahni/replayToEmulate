package acuity.replay

import org.joda.time.format.DateTimeFormat
import scala.reflect.runtime.universe

object BlobStructure {

  case class EhRecord(SequenceNumber: Long,
                      Offset: String,
                      EnqueuedTimeUtc: String,
                      SystemProperties: Map[String, String],
                      Properties: Map[String, String],
                      Body: String)

  def apply(acuityConfig: Configuration.Acuity): List[String] = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    runtimeMirror.reflectModule(runtimeMirror.staticModule(acuityConfig.blob.structure))
      .instance.asInstanceOf[BlobStructure.StructureInterface](acuityConfig)
  }

  trait StructureInterface {

    def apply(acuityConfig: Configuration.Acuity): List[String]
  }

  object EventHubCapture extends StructureInterface {
    def apply(acuityConfig: Configuration.Acuity): List[String] = {
      val format = DateTimeFormat.forPattern("yyyy-MM-dd")
      // currently 1 pod per partition is run, deployment is expected to be a stateful e.g. pod names xyz-0
      val partition = if (acuityConfig.podName.contains("-")) acuityConfig.podName.split("-").last.toInt else 0
      Iterator.iterate(format.parseDateTime(acuityConfig.startDate))(_.plusDays(1))
        .takeWhile(_.isBefore(format.parseDateTime(acuityConfig.endDate)))
        .map(d => s"$partition/${"%04d".format(d.getYear)}/${"%02d".format(d.getMonthOfYear)}/${"%02d".format(d.getDayOfMonth)}")
        .toList
    }
  }

}