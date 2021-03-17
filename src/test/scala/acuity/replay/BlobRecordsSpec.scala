package acuity.replay

import acuity.replay.BlobRecords.EhRecord
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.ConfigSource
import pureconfig.generic.auto._

class BlobRecordsSpec extends AnyFlatSpec {

  private val config = ConfigFactory.load()
  private val acuityConfig = ConfigSource.fromConfig(config.getConfig("acuity")).loadOrThrow[Configuration.Acuity]

  it should "convert eh record to kafka message" in {

    val record = EhRecord(1234,
      "Offset",
      "EnqueuedTimeUtc",
      Map.empty,
      Map.empty,
      """{"test": "test"}""")
    val kafka = record.toKafka(acuityConfig.copy(kafka = acuityConfig.kafka.copy(keyPath = "$.test", outputTopic = "topic")))
    assert(kafka.isDefined && kafka.get.value() == """{"test": "test"}""")
    assert(kafka.isDefined && kafka.get.topic() == "topic")
    assert(kafka.isDefined && kafka.get.key().sameElements("test".getBytes("UTF8")))
  }

  it should "convert eh record to kafka message - nested key" in {

    val record = EhRecord(1234,
      "Offset",
      "EnqueuedTimeUtc",
      Map.empty,
      Map.empty,
      """{"test": {"key": "key"}}""")
    val kafka = record.toKafka(acuityConfig.copy(kafka = acuityConfig.kafka.copy(keyPath = "$.test.key", outputTopic = "topic")))
    assert(kafka.isDefined && kafka.get.value() == """{"test": {"key": "key"}}""")
    assert(kafka.isDefined && kafka.get.topic() == "topic")
    assert(kafka.isDefined && kafka.get.key().sameElements("key".getBytes("UTF8")))
  }

  it should "fallback to hashcode when key cannot be found in json" in {

    val record = EhRecord(1234,
      "Offset",
      "EnqueuedTimeUtc",
      Map.empty,
      Map.empty,
      """{"test": "test"}""")
    val kafka = record.toKafka(acuityConfig.copy(kafka = acuityConfig.kafka.copy(keyPath = "$.test.key", outputTopic = "topic")))
    assert(kafka.isDefined && kafka.get.value() == """{"test": "test"}""")
    assert(kafka.isDefined && kafka.get.topic() == "topic")
    assert(kafka.isDefined && kafka.get.key().sameElements("""{"test": "test"}""".hashCode.toString.getBytes("UTF8")))
  }

  it should "fail to convert non json message" in {

    val record = EhRecord(1234,
      "Offset",
      "EnqueuedTimeUtc",
      Map.empty,
      Map.empty,
      """blah""")
    val kafka = record.toKafka(acuityConfig.copy(kafka = acuityConfig.kafka.copy(keyPath = "$.test.key", outputTopic = "topic")))
    assert(kafka.isEmpty)
  }
}