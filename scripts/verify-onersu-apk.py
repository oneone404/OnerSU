#!/usr/bin/env python3
"""Validate the native payload and single-APK packaging of a release build."""
import hashlib
from pathlib import Path
import re
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
apk = Path(sys.argv[1])
with zipfile.ZipFile(apk) as bundle:
    names = bundle.namelist()
    assert not any(name.lower().endswith(".apk") for name in names), "Nested APK found"
    abis = {name.split("/")[1] for name in names if name.startswith("lib/")}
    assert abis == {"arm64-v8a"}, abis
    backend = bundle.read("lib/arm64-v8a/libksud.so")
    assert hashlib.sha256(backend).hexdigest() == "e9b979887f5cc99747d3c12765f9afc18272e67c2144f5ad93b246c5b3598e6a"
    native = bundle.read("lib/arm64-v8a/libexp.so")
    assert native[:4] == b"\x7fELF"
    assert b"Java_df_root_ExploitRunner_nativeRunAll" in native
    ko = (ROOT / "manager/app/src/main/cpp/dfroot/ko/dirtyfrag-android16-6.12.ko").read_bytes()
    assert ko in native, "Audited bootstrap module missing from libexp"
    assert b"/data/user_de/0/com.onersu.onersu/ksud\x00" in ko
    assert b"/data/user_de/0/df.root/" not in native, "Old app path still embedded"
    shim = bundle.read("assets/ksud")
    assert b"\r" not in shim, "Bootstrap must have Unix line endings"
    for placeholder in (b"@KSUD@", b"@APK@", b"@READY@", b"@RUN_ID@"):
        assert placeholder in shim
    assert b"am force-stop com.onersu.onersu" in shim
    assert b"com.onersu.onersu/org.bakasu.bakasu.ui.MainActivity" in shim
    dex = b"".join(bundle.read(name) for name in names if name.endswith(".dex"))
    assert b"Ldf/root/ExploitRunner;" in dex and b"Ldf/root/IReporter;" in dex
aapt2 = sys.argv[2]
resources = subprocess.check_output([aapt2, "dump", "resources", str(apk)], encoding="utf-8")
icon = re.search(r"resource [^\n]*mipmap/ic_onersu\n[^\n]*\(file\) ([^\s]+)", resources)
assert icon, "Launcher icon resource missing"
tree = subprocess.check_output([aapt2, "dump", "xmltree", str(apk), "--file", icon.group(1)], encoding="utf-8")
assert "E: adaptive-icon" in tree, "Launcher icon is not adaptive"
print("PASS: one APK, arm64, pinned backend, JNI runner, audited module, valid shim")
print("APK SHA-256:", hashlib.sha256(apk.read_bytes()).hexdigest())
