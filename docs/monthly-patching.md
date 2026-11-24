# Monthly patching

Add a dated section after each month's dependency review. Record resolved versions, the reason for each change, and any findings left for follow-up.

## 2026-10-01

| Dependency | From | To | Why |
| --- | --- | --- | --- |
| Jackson 2 BOM (`jackson-2-bom.version`) | 2.21.5 | 2.21.7 | Fix five `com.fasterxml.jackson.core:jackson-databind` advisories covering denial of service and unsafe deserialization behavior. |
| Jackson 3 BOM (`jackson-bom.version`) | 3.1.5 | 3.1.7 | Fix the corresponding five `tools.jackson.core:jackson-databind` advisories. |

The resolved Gradle runtime dependencies were checked with OSV Scanner after the updates; no advisories remained. Unit tests passed before and after patching with JDK 25.

## 2026-11-05

| Dependency (Gradle buildscript classpath) | From | To | Why |
| --- | --- | --- | --- |
| `org.apache.commons:commons-lang3` | 3.16.0 | 3.18.0 | Fixes CVE-2025-48924 / GHSA-j288-q9x7-2f5v (moderate). |

Audit command: `osv-scanner scan source --lockfile <resolved.cdx.json> --format json --all-vulns --all-packages` (before scans used the equivalent, deprecated `--sbom` option). Resolved runtime and buildscript classpaths were exported as CycloneDX 1.5 SBOMs (143 and 39 modules); all severity levels and DoS advisories were included. The buildscript scan went from one moderate finding to none; the runtime scan found no advisories before or after patching. The baseline and post-patch test/report runs passed with 4 tests, 0 skipped, each run. The post-patch `./gradlew build` also passed. Gradle wrapper distribution and JDK versions are not represented in the Maven SBOM inventory; they were not covered by this OSV scan. No dependency findings were deferred.

## 2026-11-24

| Dependency | From | To | Why |
| --- | --- | --- | --- |
| Gradle versions plugin | 0.64.0 | 0.67.0 | Update the dependency-update reporting plugin. |
| `org.apache.tika:tika-core` | 4.0.+ (resolved to 4.0.0) | 4.2.0 | Update Apache Tika and pin its version. |
| CycloneDX Gradle plugin | Not present | 3.6.1 | Generate an SBOM of resolved runtime and buildscript dependencies. |
| `com.google.guava:guava` (buildscript) | 33.7.1-jre | 33.7.2-jre | Fix CVE-2026-102554 / GHSA-xxph-c9ww-hj94 in a transitive dependency of the CycloneDX plugin. |

The combined SBOM was checked with OSV Scanner against a local vulnerability database. The scan found one Guava advisory before the override and none among the 223 packages afterwards. The JDK and Gradle distribution are not covered. CycloneDX reported unavailable Maven metadata for several Gradle plugins.

SBOM generation is opt-in via `./gradlew -Psbom cyclonedxDirectBom`; standard builds do not generate or package an SBOM. Both the standard build and the on-demand SBOM generation were verified.

Replaced the deprecated Spring HTTP status names `PAYLOAD_TOO_LARGE` and `UNPROCESSABLE_ENTITY` with `CONTENT_TOO_LARGE` and `UNPROCESSABLE_CONTENT`; the response codes remain 413 and 422. The build and tests passed with JDK 25, and recompilation with deprecation warnings enabled reported no warnings.