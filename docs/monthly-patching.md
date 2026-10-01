# Monthly patching

Add a dated section after each month's dependency review. Record resolved versions, the reason for each change, and any findings left for follow-up.

## 2026-10-01

| Dependency | From | To | Why |
| --- | --- | --- | --- |
| Jackson 2 BOM (`jackson-2-bom.version`) | 2.21.5 | 2.21.7 | Fix five `com.fasterxml.jackson.core:jackson-databind` advisories covering denial of service and unsafe deserialization behavior. |
| Jackson 3 BOM (`jackson-bom.version`) | 3.1.5 | 3.1.7 | Fix the corresponding five `tools.jackson.core:jackson-databind` advisories. |

The resolved Gradle runtime dependencies were checked with OSV Scanner after the updates; no advisories remained. Unit tests passed before and after patching with JDK 25.
