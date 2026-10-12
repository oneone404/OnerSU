# OnerSU 0.1.0 build validation

Date: 2026-10-12. Built with Java 21, NDK 30.0.16248370, Android compile SDK 37, build tools 36.1.0.

| Check | Result |
| --- | --- |
| `spotlessCheck` | Pass |
| `assembleRelease` | Pass |
| `lintRelease` | Pass, 0 errors and 53 warnings in the inherited manager/resources |
| Mock bootstrap handoff | Pass: waits for driver registration, rejects stale cleanup token, restarts only after current cleanup token, exits on late-load failure |
| APK package and label | `df.root`, OnerSU, version 0.1.0 / 40001, arm64-v8a |
| APK signing | RSA 3072, APK signature schemes v2 and v3 verified |

Signed APK: `OnerSU_0.1.0_40001-arm64-v8a-release.apk`.

- APK SHA-256: `13aae5fbefd7831266c116b3b773ed978d84fa6b1a9c62d08857895287f5748b`.
- Public signing certificate SHA-256: `e7e5c7678fc574f768872f9a980bd0e315dd8b2d14c7e96ec014044fe12b7b15`.
- Signing key/passwords are stored outside this repository and are not published.

The merged root flow has not yet been executed on a phone. These are build and packaging checks, not proof of root success, superuser grants, SELinux token access or persistent root on hardware. The manual GitHub workflow produces unsigned builds; its result must not be confused with this locally signed APK.
