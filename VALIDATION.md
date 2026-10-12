# OnerSU build and device validation

## 0.1.5 OPPO installer policy

Date: 2026-10-12. APK source commit `56fb08c`, version code 40010.

- Added the first row in Settings → App settings, using the existing settings widget, repository/use-case/ViewModel and Koin conventions. Resources are provided in English, Vietnamese and Simplified Chinese.
- The preference records intent. Native manager access plus an actual root shell are required to apply it. The displayed `Blocked` result requires verified package `disabled-user` and disabled `InstallStart`, with a saved baseline for the current Android user. No alternative installer is required.
- Reconciles on manager initialization/root-screen resume and settings resume. A root-owned boot-completed hook applies the policy at boot completion and again after 5 and 25 seconds, then exits. Disabling removes the hook before restoration; a failed restoration retains its snapshot for retry. Disabling without root records a pending restoration.
- Device validation of the final current-user script: only user 0 was snapshotted and modified. Users 11 and 999 retained their existing states and no explicit InstallStart overrides were added for them. Disabling restored user 0 to its saved `installed=true`, package default, component default state; the boot hook was absent afterwards.
- One actual Android soft restart verified the shared boot hook and all three bounded passes before the scope was narrowed from all users to the current user. The kernel boot ID was unchanged, root remained available and SELinux remained Enforcing. The APK intent query excluded the stock OPPO installer. No additional restart was performed after narrowing; the final selection/restore logic was tested directly on the running phone.
- Final `spotlessCheck`, `assembleRelease`, `lintRelease`: pass; 0 lint errors and 56 inherited warnings. Signed-package verification passes, including byte-for-byte comparison of the shipped installer policy asset with its source.
- Signed APK: `OnerSU_0.1.5_40010-arm64-v8a-release.apk`; SHA-256 `1483083e03e4d2a1ef632622ef36f1714eb1a4701a813221896b95c8e58d362e`. V2-only, same 971-byte certificate and pinned root backend as 0.1.4.
- No new root exploit or full device reboot was run. The new manager APK was not installed during this verification pass; delivery is via the existing stable online updater. The device test policy was disabled and its baseline restored at the end. Full reboot still loses temporary root; arbitrary later OEM state changes are handled on the next reconciliation, not by a permanent background watcher.

## Historical 0.1.2 validation

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

## 0.1.3 root result and manual soft restart

Date: 2026-10-12. APK built from source commit `b3edc34`, version code 40005.

- `spotlessCheck`, `assembleRelease`, and `lintRelease`: pass; lint has 0 errors and 56 inherited warnings.
- The bootstrap records successful dynamic-manager registration with the current run UUID. Java clears the record using an app-owned file so its SELinux label is retained. Native timeout is a pending confirmation, not a root failure verdict. The repository waits for matching registration evidence after native cleanup.
- The hook mutex alone never proves root. The soft restart button requires native manager access and a matching UAPI; driver presence without manager readiness has a separate status.
- Mocked real bootstrap: pass for delayed registration, stale cleanup token rejection, fresh registration evidence, reopening the root log after cleanup, late-load failure, and rejected manager registration. Failure cases do not write a success token or restart the manager.
- Signed release package verification: pass; V2-only signature, same 971-byte certificate as 0.1.2, same pinned backend/KO, updated shim and neutral native timeout text.
- Updated the plugged-in PME110 in place without clearing application data. Package reports 0.1.3 / 40005. Root log screen shows confirmed manager access and the manual soft restart button at the lower right. The app process holds FD 6, `anon_inode:[ksu_driver]`. `su -c id` remains uid 0 and SELinux remains Enforcing.
- Copied to `/sdcard/Download/OnerSU_0.1.3.apk`; device hash matches the PC artifact. APK SHA-256: `cb9ecda3d7dda1a35ed08847caaf5b13b5e7085ada80c311eb37b0c5c06df3c7`.
- No root exploit or Android restart was executed during the 0.1.3 update. The new fresh-boot root/registration/reopen sequence still needs device validation. The soft restart action reuses the existing backend route that the user successfully used with ReZygisk and HMA before this update.

The user's SUSFS ZIP, `susfs-lkm-module-0.2.0-arm64-multi-kmi.zip`, only packages KMIs through android15-6.6. Its `select_kmi` rejects this phone's android16-6.12 kernel. No saved SUSFS installation log was found; this mismatch was verified from the actual ZIP installer and KMI list. No attempt was made to load a mismatched module.

## 0.1.4 soft restart availability and signed release

Date: 2026-10-12. APK source commit `85891bd`, version code 40008.

- Corrected the state mismatch where `Completed` was confirmed by the current registration token after native cleanup, but `canSoftReboot` remained false because the original process did not yet hold its driver FD. Availability now derives from the trusted `Completed` or `AlreadyLoaded` status rather than a separately latched flag. Mere module presence and historical logs cannot enable the button.
- Refreshes manager readiness on root-screen resume through a repository/use-case/ViewModel boundary. Checks the soft restart backend's exit result so rejected requests become a visible failure.
- The home button and root-page title read `DFROOT` in English, Vietnamese and Simplified Chinese resources; confirmed in the final APK resource table.
- Final `spotlessCheck`, `assembleRelease`, `lintRelease`: pass; 0 lint errors and 56 inherited warnings. Mock bootstrap handoff checks: pass. Existing package verification: pass for pinned backend/module, native runner, shim, icon, package and release signing identity.
- Signed APK: `OnerSU_0.1.4_40008-arm64-v8a-release.apk`; SHA-256 `a6fb52dafa7c1d8019d1188222db8fc22c3d8d86bd54eb1f5d8b5c31dd8b74e5`. V2-only signature, same 971-byte certificate as 0.1.3, SHA-256 `1f14b12db494dcaebd8edeada2805bd6c5e45fbe1cda89c39194ff3b642294db`.
- Connected phone still runs 0.1.3 / 40005 with working root and a manager driver FD. No fresh exploit run, Android restart, or 0.1.4 installation was performed during this repair; the user requested delivery via the online updater. Fresh-boot 0.1.4 behavior remains untested on hardware.
- The stable updater already targets `oneone404/OnerSU` and accepts the released asset's filename and higher version code. The manual CI workflow still produces unsigned artifacts; the stable release is signed locally with the private key kept outside this repository.
