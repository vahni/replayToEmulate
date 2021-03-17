package acuity.replay

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.format.DateTimeFormat

import scala.reflect.runtime.universe

object BlobStructure extends LazyLogging {
  /**
   * Load the configured structure object
   *
   * @param acuityConfig configuration object
   * @return list of folders
   */
  def apply(acuityConfig: Configuration.Acuity): List[String] = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    runtimeMirror.reflectModule(runtimeMirror.staticModule(acuityConfig.blob.structure))
      .instance.asInstanceOf[BlobStructure.StructureInterface](acuityConfig)
  }

  /**
   * Structure interface
   *
   * a new structure must extend this interface and produce a list of traversable folder paths
   */
  trait StructureInterface {

    def apply(acuityConfig: Configuration.Acuity): List[String]
  }

  /**
   * Event hub capture structure
   *
   * Event hubs store the data as partition/year/month/day/hour/min
   */
  object EventHubCapture extends StructureInterface {
    def apply(acuityConfig: Configuration.Acuity): List[String] = {
      val format = DateTimeFormat.forPattern("yyyy-MM-dd")
      // currently 1 pod per partition is run, deployment is expected to be a stateful e.g. pod names xyz-0
      val partitions = 0 until acuityConfig.partitions
      logger.info(s"Using partitions $partitions")
      partitions.map { p =>
        Iterator.iterate(format.parseDateTime(acuityConfig.startDate))(_.plusDays(1))
          .takeWhile(_.isBefore(format.parseDateTime(acuityConfig.endDate)))
          .map(d => s"$p/${"%04d".format(d.getYear)}/${"%02d".format(d.getMonthOfYear)}/${"%02d".format(d.getDayOfMonth)}")
          .toList
      }.toList.flatten
    }
  }

}