package bdtlab.replay

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.ConfigSource
import pureconfig.generic.auto._

class BlobStructureSpec extends AnyFlatSpec {

  private val config = ConfigFactory.load()
  private val bdtlabConfig = ConfigSource.fromConfig(config.getConfig("bdtlab")).loadOrThrow[Configuration.Bdtlab]

  it should "produce a folder for each day" in {

    val result = BlobStructure(bdtlabConfig.copy(startDate = "2021-01-01", endDate = "2021-01-10"))

    assert(result.length == 9)
    assert(result.head == "0/2021/01/01")
    assert(result.last == "0/2021/01/09")
  }

  it should "produce a folder from multiple partitions" in {

    val result = BlobStructure(bdtlabConfig.copy(startDate = "2021-01-01", endDate = "2021-01-10", partitions = 2))

    assert(result.length == 18)
    assert(result.head == "0/2021/01/01")
    assert(result.last == "1/2021/01/09")
  }
}