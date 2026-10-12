#!/usr/bin/env python3
"""Align and sign a manager release with the V2-only format required by KernelSU."""
import argparse
from pathlib import Path
import subprocess
import tempfile

from apk_signature import check_manager_signature


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--build-tools", type=Path, required=True)
    parser.add_argument("--keystore", type=Path, required=True)
    parser.add_argument("--ks-pass-file", type=Path, required=True)
    parser.add_argument("--key-pass-file", type=Path, required=True)
    parser.add_argument("--alias", default="onersu")
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    zipalign = args.build_tools / ("zipalign.exe" if (args.build_tools / "zipalign.exe").exists() else "zipalign")
    signer = [args.java, "-jar", str(args.build_tools / "lib/apksigner.jar")]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="onersu-sign-") as folder:
        aligned = Path(folder) / "aligned.apk"
        subprocess.run([str(zipalign), "-P", "16", "-f", "4", str(args.apk), str(aligned)], check=True)
        subprocess.run(signer + [
            "sign", "--ks", str(args.keystore), "--ks-key-alias", args.alias,
            "--ks-pass", "file:" + str(args.ks_pass_file),
            "--key-pass", "file:" + str(args.key_pass_file),
            "--v1-signing-enabled", "false", "--v2-signing-enabled", "true",
            "--v3-signing-enabled", "false", "--v4-signing-enabled", "false",
            "--out", str(args.output), str(aligned),
        ], check=True)
    subprocess.run(signer + ["verify", "--verbose", str(args.output)], check=True)
    size, digest = check_manager_signature(args.output)
    print(f"PASS: KernelSU-compatible V2-only signature, certificate size={size}, SHA-256={digest}")


if __name__ == "__main__":
    main()
