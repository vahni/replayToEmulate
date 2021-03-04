# Introduction 
Stream data from blob storage to Kafka

# Getting Started
- Export env variables for the blob storage credentials and configuration
  - e.g. `export BLOB_ACCOUNT=xyz`
- TODO Start Kafka and create a topic

# Build and Test
* Running locally: `sbt clean run`
* Build a fat jar: `sbt clean assembly`
* Run unit tests with coverage `sbt clean coverageOn coverage test coverageReport`

# Configurability

### Defining a new blob folder structure 
Create a new object in `BlobStructure`.  This object should define the folder structure and return folders within 
the date range specified.  The discovery task will find all blobs that live under these folders.

### Defining a new blob data format
Create a new object in 'BlobStreamer'.  This object takes a blob file and creates a iterator of a user defined object type.
This iterator is used to send data to Kafka via an Akk stream.

In addition to above you must define a conversion routing to convert a blob entity to Kafka message TODO