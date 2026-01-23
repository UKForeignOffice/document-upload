# Document Upload Service — API Specification

## Overview

A small HTTP API for uploading, retrieving and deleting documents. Uploaded files are scanned for viruses, validated for type and image quality (OCR), and stored. Multiple files may be uploaded together and merged by a configured `Merger` implementation.

Base URL: the application root (e.g. `https://{host}:{port}`)

Timeouts: requests that require longer processing (virus scan, merging, storage) may return `202 Accepted` if processing exceeds the server timeout (2 minutes).

Authentication: none is implemented in the codebase. Add an authentication layer (reverse proxy or middleware) if required.

Notes:
- Multipart field name: `files` (supports multiple files)
- Per-file max size: documented as 5MB in API annotations (enforce at proxy or controller-level as needed)
- Server-side responses may include short text codes in the body such as `fileTypeError`, `virusError`, and `qualityWarning`.

## Endpoints

- **POST /files** — Store one or more files
  - Description: Scans uploaded files (antivirus, file-type), optionally runs OCR image-quality checks, merges multiple files when needed, stores the result and returns the retrieval URL.
  - Request:
    - Content-Type: `multipart/form-data`
    - Field: `files` — binary file(s). Send one or more instances of the field to upload multiple files.
    - Each file is treated as binary (`format: binary`). The controller's parameter allows `List<MultipartFile>`.
  - Important behavior:
    - File type and MIME are validated; invalid types produce `400 Bad Request` with body `fileTypeError`.
    - Files are scanned by the antivirus service — infected files produce `422 Unprocessable Entity` with body `virusError`.
    - OCR-based quality checks may fail; when they do the file is still stored but the API returns `201 Created` with body `qualityWarning` and a `Location` header pointing to the resource.
    - If processing takes too long (beyond the 2 minute server timeout), the request may return `202 Accepted` indicating asynchronous processing.
  - Responses:
    - `201 Created` — file stored successfully; header `Location: /files/{id}` points to the resource. Body may be empty or contain `qualityWarning`.
    - `202 Accepted` — request accepted but processing still ongoing (timeout).
    - `400 Bad Request` — no file uploaded, or unsupported file type. Body: `fileTypeError`.
    - `422 Unprocessable Entity` — virus detected. Body: `virusError`.
    - `500 Internal Server Error` — storage or processing failures.

- **GET /files/{id}** — Retrieve a stored file
  - Description: Streams the binary content of the stored file.
  - Request:
    - Path parameter: `id` (string)
  - Responses:
    - `200 OK` — binary payload is streamed. Headers set by server include `Content-Length` and `Content-Type` based on stored metadata.
    - `404 Not Found` — no file with that id exists.
    - `500 Internal Server Error` — error reading from storage.

- **DELETE /files/{id}** — Delete a stored file
  - Description: Removes the file from storage.
  - Request:
    - Path parameter: `id` (string)
  - Responses:
    - `204 No Content` — deletion successful.
    - `500 Internal Server Error` — deletion failed.

- **GET /health** — Health check
  - Description: Simple probe endpoint used by load balancers.
  - Responses:
    - `200 OK` — service is running.

## Example Requests

Upload a single file:

```bash
curl -v -F "files=@/path/to/document.pdf" https://api.example.com/files
```

Upload multiple files (they may be merged by the server):

```bash
curl -v \
  -F "files=@/path/to/document1.pdf" \
  -F "files=@/path/to/document2.pdf" \
  https://api.example.com/files
```

Retrieve a file:

```bash
curl -v -o saved-file.bin https://api.example.com/files/<id>
```

Delete a file:

```bash
curl -v -X DELETE https://api.example.com/files/<id>
```

## Error codes and bodies

- `fileTypeError` (400) — file type not supported or missing files.
- `virusError` (422) — antivirus scan failed, file contains a virus.
- `qualityWarning` (201 with Location header) — file passed scanning but failed OCR-based quality; stored but quality issues present.
- `404` — resource not found when retrieving files.
- `500` — server/storage error.
