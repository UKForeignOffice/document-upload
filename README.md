# Document Upload

Document Upload service that accepts any file, scans it for viruses and then 
either stores it or rejects it.

## Goal
This is an intentionally very simple service for storing files as there is cross over in 
requirements with ETD. Work needs to be done to review and potentially consolidate the two 
projects.

### Architecture diagram

Partial C4 Container model diagram of the Notarial Marriage service.

```mermaid
C4Container
    title Container Diagram - FCDO Marriage Service

    System_Boundary(marriageService, "FCDO Marriage Service") {
        System(existing, "Existing CYB Marriage Frontend", "Existing web application providing <br> journey for marriage document applications")
        
        System(frontend, "Notarial Marriage Frontend", "Web application providing journey <br> for marriage document applications")

        System(documentUpload, "Document Upload", "Handles file uploads and document")
    }

    Rel(applicant, frontend, "Applies for marriage document", "HTTPS")
    Rel(frontend, existing, "Redirects", "HTTPS")
    Rel(frontend, documentUpload, "Uploads supporting documents", "HTTPS/API")
    Rel(existing, documentUpload, "Redirects", "HTTPS")
    Rel(existing, documentUpload, "Uploads supporting documents", "HTTPS/API")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

## Setting up the service

### Configuration

Application configuration can be found in `src/main/resources/application.yml`. Some of these properties 
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

### Installation

This repository is private, you will need access to UKForeignOffice to access it.

```bash
git clone https://github.com/UKForeignOffice/document-upload.git

cd document-upload
```

#### Local development

In `Dockerfile`, change the image to `cimg/openjdk:17.0.11`.

In `docker-compose.yml`, change the `clamav` service `image: clamav/clamav:1.4.3`.
In `docker-compose.yml`, add to the `clamav` service `platform: linux/amd64`.

```bash
./gradlew clean build
```

## Using the service

### Run

```bash
docker compose up --build
```

### Endpoints

1. `GET` `/v1/files/{id}` - Get a file by id

```bash
curl http://localhost:9000/v1/files/4ec76a9f310e0ee531.jpg
```

2. `POST` `/v1/files` - Scans and stores the files for viruses

```bash
curl -X POST -F \
"files=@/path/to/file/4ec76a9f310e0ee531.jpg" \
http://localhost:9000/v1/files
```

## AntiVirus

Scanning inbound files for viruses is supported by integration with ClamAV. An example
ClamAV service is configured in `docker-compose.yml`. 

## Permissions

There are no explicit access controls to this service. Knowledge of a file ID is 
considered permission to manage that file, e.g. delete it.

## Deployment

The service should be packaged and deployed via Docker.
