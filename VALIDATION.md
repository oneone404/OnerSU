# OnerSU 0.1.2 build and device validation

Date: 2026-10-12. Java 21, NDK 30.0.16248370, Android compile SDK 37, build tools 36.1.0.

| Check | Result |
| --- | --- |
| `spotlessCheck` | Pass |
| `assembleRelease` | Pass |
| `lintRelease` | Pass, 0 errors and 56 inherited warnings |
| Mock bootstrap handoff | Pass: waits for registration, rejects a stale cleanup token, restarts after cleanup, exits on late-load failure |
| APK package and label | `com.onersu.onersu`, OnerSU, 0.1.2 / 40004, arm64-v8a |
| APK signing | RSA 3072, V2 only; apksigner verified; V1/V3/V3.1/V4 absent |
| Driver certificate format | 971-byte DER certificate, below the 1024-byte kernel limit |
| Release identity | The compiled manager's signature check matches the APK certificate |
| Launcher icon | Original supplied image, adaptive mask retained, 1/6 proportional inset on each foreground edge |
| Bootstrap tools | System-first PATH; `/system/bin/sleep` completed on the rooted phone with SELinux Enforcing |
| Phone registration | Dynamic-manager API returned the expected certificate; kernel crowned OnerSU |
| Manager process | Relaunched app inherited `anon_inode:[ksu_driver]` |
| Visible phone status | Home showed “Đang hoạt động”, LKM, Jailbreak mode, `SuperUser: 2, Modules: 0`; incorrect unofficial-manager warning absent |
| Root shell | `uid=0(root)`, `u:r:ksu:s0`; SELinux Enforcing |
| APK delivery | Installed and copied to `/sdcard/Download/OnerSU_0.1.2.apk`; device hash matched |

Signed APK: `OnerSU_0.1.2_40004-arm64-v8a-release.apk`.

- APK SHA-256: `130282a7ac4b5f1175022849fe771880ad2862ee16b4b33d7fca030e39788587`.
- Public signing certificate SHA-256: `1f14b12db494dcaebd8edeada2805bd6c5e45fbe1cda89c39194ff3b642294db`.
- Running PME110 kernel: `6.12.58-android16-6-g7704a1ae279b-ab15213644-4k`.
- Running driver: `v4.2.0-rc3-f6cb7e70@BakaSU`, version 35226, UAPI 5, LKM late-load mode.
- Signing key/passwords and private rollback backup are outside the repository.

## Corrected failures and migration

The earlier APK used V3 and a 1048-byte certificate. `ksud set-apk` rejected V3; the kernel's certificate parser independently rejects lengths above 1024 bytes. Thus root had loaded while OnerSU was not registered as a manager. The short certificate uses the same RSA private key with a shorter subject, but Android treats its different certificate bytes as a new APK signing identity.

Before reinstalling, the actual installed APK and four application-data archives for Android users 0 and 11 were backed up privately. Eleven restored data files were hash-verified. Ownership was updated for the new UID and application SELinux labels were restored using `restorecon -RFD`. Subsequent APK updates used the same 971-byte certificate and preserved data normally. The standalone `df.root` package remained absent. The bootstrap also now selects system tools because the inherited vendor-first PATH caused vendor sleep linker failures in the previous run.

## Verification boundary

The root exploit was not rerun and the phone was not rebooted during this repair. Registration and manager operation were verified in the already active BakaSU root session. A fresh-boot DFRoot attempt with 0.1.2 and the cleanup-token handoff still need hardware validation. Root remains temporary; persistent root and bootloader unlock are not implemented. The manual GitHub workflow produces unsigned builds.
