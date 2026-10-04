# Security Policy

## Supported versions

Only the latest release of SpaceSaver receives security fixes.

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Report vulnerabilities privately through GitHub's
[private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
("Report a vulnerability" on the repository's **Security** tab).

Please include:

- The affected version and Android version.
- Steps to reproduce, or a proof of concept.
- The impact you believe it has (for example data loss, files deleted without consent, or data leaving the device).

You can expect an acknowledgement within 7 days. We will keep you updated while we work on a fix and credit you in the release notes unless you prefer otherwise.

## Scope

SpaceSaver is offline by design: it declares no `INTERNET` permission and never requests
`MANAGE_EXTERNAL_STORAGE`. Issues we treat as security-relevant include:

- Any path that lets media or metadata leave the device.
- Any path that deletes or overwrites a user's original files without the explicit review and system confirmation described in the README.
- Exposed components (activities, providers, receivers) that other apps can abuse.
