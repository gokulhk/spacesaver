# ADR-0004: SI byte units (1 KB = 1,000 bytes)

- **Status:** Accepted
- **Date:** 2026-10-04

## Context

Byte sizes can be shown in SI units (1 GB = 10⁹ bytes) or binary units (1 GiB = 2³⁰ bytes). Android's Settings > Storage screen and `Formatter.formatFileSize` use SI units on modern versions. If SpaceSaver used binary units, its "free space" and "saved" numbers would disagree with the system by about 7%, which undermines trust.

## Decision

Use SI units everywhere:

- `ByteSize` is a value class wrapping a non-negative `Long` byte count.
- 1 KB = 1,000 B, 1 MB = 10⁶ B, 1 GB = 10⁹ B, 1 TB = 10¹² B.
- Formatting: no decimals for bytes and KB (`"999 B"`, `"512 KB"`), one decimal for MB and above (`"1.4 MB"`, `"12.4 GB"`).

## Consequences

- Numbers match the system storage screen.
- Sizes never travel as raw `Long`s in signatures, which avoids unit mix-ups.
- Formatting rules are unit-tested at the rounding edges (plan Task 2.1).
