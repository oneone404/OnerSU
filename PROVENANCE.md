# Source and binary provenance

## Manager and backend

- Source: https://github.com/Baka-SU/BakaSU
- Commit: `f6cb7e70c10ebde149f3121ae68b945514fd0801`
- Official manager build run: https://github.com/Baka-SU/BakaSU/actions/runs/38152678560
- Artifact: `11707433608`, `ksud-aarch64-linux-android`.
- ZIP SHA-256: `c2312f5d9940bee6662951788d829eb2bb9fa5466e2eacb07c74ea977d402971` (matches GitHub artifact API digest).
- Executable SHA-256: `e9b979887f5cc99747d3c12765f9afc18272e67c2144f5ad93b246c5b3598e6a`.
- Extracted executable is packaged as `lib/arm64-v8a/libksud.so`. It contains upstream kernel assets; it is not a JNI shared library.
- Corresponding kernel/userspace source is retained in this repository. Upstream build instructions/scripts are retained; inherited release/bot automation was replaced with a manual OnerSU build.

## DFRoot

- Source: https://github.com/Applying1/DFRootOPLUS
- Snapshot commit: `7a3ee2b077f963e043fcdf0a9fd04492656a126c`, plus local PME110 patches previously verified in the separate DFRoot APK.
- Original 0.1.0 bootstrap SHA-256: `26cb2f3e0935b66c119f517c6bdf92ac2b5ad957aabc8525994ea6d708050d0d`.
- Current 0.1.1 bootstrap SHA-256: `898dda48e11c32f9a74bea00ce1d42f2dc7caa946f25ca8518f2c525411fc94c`.
- Current module was compiled from `dfroot/dirtyfrag-lkm/dirtyfrag.c` with package path `/data/user_de/0/com.onersu.onersu/ksud`. Build: https://github.com/oneone404/OnerSU/actions/runs/38251557029 at source commit `a4c3743`.
- DDK image: `ghcr.io/ylarod/ddk-min@sha256:fd3ec5cc71d7f2716212e9df8d5674a9afe68fb769282fcac87b83f207f949ee`.
- Module vermagic remains `6.12.76-4k SMP preempt mod_unload modversions aarch64`, matching the original bootstrap. Only this android16-6.12 bootstrap is embedded in the APK; other original KMI binaries are retained as reference assets.
- The helper is rebuilt from `splicehelper.c`; no separate DFRoot APK is included.
- Native integration changes: CMake subdirectory paths, C23-compatible `__asm__`, absolute binary inclusion paths required for LTO, and separate manager/DFRoot build targets. The root primitive and restoration routines are retained.
- Java integration adds a shim, a unique per-run cleanup token and manager registration/restart ordering. Soft reboot is disabled for the merged flow.

## Validation boundary

PC checks cover compilation, Android lint, Kotlin formatting, mock backend/cleanup ordering, APK contents and signing. They do **not** validate the new merged app on hardware, driver compatibility after loading, Android SELinux access to the cleanup token, or installation over another signing key. No persistent-root or bootloader-unlock support is claimed.
