# Introduction 
Stream data from blob storage to Kafka

# Getting Started
- Export env variables for the blob storage credentials and configuration
  - e.g. `export BLOB_ACCOUNT=xyz`
- Start Kafka
  - `docker run -d --name my-kafka -p 2181:2181 -p 9092:9092 --env "ADVERTISED_HOST=`ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p'`" --env "ADVERTISED_PORT=9092" spotify/kafka`

# Build and Test
* Running locally: `sbt clean run`
* Build a fat jar: `sbt clean assembly`
* Run unit tests with coverage `sbt clean coverageOn coverage test coverageReport`

# Configurability

### Defining a new blob folder structure 
Create a new object in `BlobStructure`.  This object should define the folder structure and return folders within 
the date range specified.  The discovery task will find all blobs that live under these folders.

### Defining a new blob data format
Create a new object in `BlobStreamer`.  This object takes a blob file and creates an iterator of a user defined object type.
This iterator is used to send data to Kafka via an Akk stream.

### Defining the Kafka conversion
Create a new object in `BlobRecords`.  This object should represent a dat record in the blob file and be used as the data type
in the `BlobStream`.  This new record must define a `toKafka` method which defines how to convert it to Kafka.

# Installing the helm chart
* `sbt clean assembly`
* `docker build -t cafcontainerregistry.azurecr.io/bdtlab-replay:{your tag} .`
* `docker push cafcontainerregistry.azurecr.io/bdtlab-replay:{your tag}`
* `helm install bdtlab-replay bdtlab-replay --set image.tag={your tag} --set blob.key="{blob key}"`
  * Many other parameters can be specified - see values.yaml for the list