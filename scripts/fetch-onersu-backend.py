#!/usr/bin/env python3
"""Fetch the pinned upstream backend; verify both archive and executable."""
import hashlib
import io
from pathlib import Path
import urllib.request
import zipfile

URL = "https://nightly.link/Baka-SU/BakaSU/actions/artifacts/11707433608.zip"
ZIP_SHA256 = "c2312f5d9940bee6662951788d829eb2bb9fa5466e2eacb07c74ea977d402971"
KSUD_SHA256 = "e9b979887f5cc99747d3c12765f9afc18272e67c2144f5ad93b246c5b3598e6a"
DEST = Path(__file__).resolve().parents[1] / "manager/app/src/main/jniLibs/arm64-v8a/libksud.so"


def main():
    if DEST.exists() and hashlib.sha256(DEST.read_bytes()).hexdigest() == KSUD_SHA256:
        print("Pinned backend already verified")
        return
    with urllib.request.urlopen(URL, timeout=120) as response:
        archive = response.read()
    if hashlib.sha256(archive).hexdigest() != ZIP_SHA256:
        raise RuntimeError("Upstream artifact digest mismatch")
    with zipfile.ZipFile(io.BytesIO(archive)) as bundle:
        backend = bundle.read("aarch64-linux-android/release/ksud")
    if hashlib.sha256(backend).hexdigest() != KSUD_SHA256:
        raise RuntimeError("Backend digest mismatch")
    DEST.parent.mkdir(parents=True, exist_ok=True)
    DEST.write_bytes(backend)
    print("Verified pinned BakaSU backend written to manager")


if __name__ == "__main__":
    main()
