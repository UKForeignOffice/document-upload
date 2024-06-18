# Document Upload

Document Upload service that accepts any file, scans it for viruses and then 
either stores it or rejects it.

## Goal
This is an intentionally very simple service for storing files as there is cross over in 
requirements with ETD. Work needs to be done to review and potentially consolidate the two 
projects.

## Configuration

Application configuration can be found in `application.yml`. Some of these properties 
can be overridden at deployment time using environment variables.  

| Environment variable   | Default                                 | Description                                                                                                                                    |
|------------------------|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| STORAGE_ENGINE         | file                                    | Either `s3` or `file`                                                                                                                          |
| S3_BUCKET              | `null`                                  | Name of the S3 bucket to store files in when using `s3` engine                                                                                 |
| ROOT_LOGGING_LEVEL     | `info`                                  | Root logging level                                                                                                                             |
| ANTIVIRUS_ENABLED      | `false` in development `true` in docker | Whether AntiVirus is enabled                                                                                                                   |
| ANTIVIRUS_HOST         | localhost                               | Network host of the antivirus service                                                                                                          |
| ANTIVIRUS_PORT         | 3310                                    | Network port of the antivirus service                                                                                                          |
| ANTIVIRUS_TIMEOUT      | 30000                                   | Virus scanning timeout                                                                                                                         |
| CONTEXT_PATH           | /v1                                     | Context path for service                                                                                                                       |
| ENABLE_QUALITY_CHECK   | false                                   | Whether to enable OCR quality check                                                                                                            |
| SHARPNESS_THRESHOLD    | 60                                      | Threshold, in %, an image's sharpness must meet to pass quality check                                                                          |
| SPRING_PROFILES_ACTIVE | `development`                           | Only needs to be set to "development" when running in docker with [localstack](https://github.com/localstack) for a mock local AWS environment |

## AntiVirus

Scanning inbound files for viruses is supported by integration with ClamAV. An example
ClamAV service is configured in `docker-compose.yml`. 

## Permissions

There are no explicit access controls to this service. Knowledge of a file ID is 
considered permission to manage that file, e.g. delete it.

## Deployment

The service should be packaged and deployed via Docker.
