"""Check the APK signing format accepted by the bundled KernelSU driver."""
import hashlib
from pathlib import Path
import struct
import zipfile

V2 = 0x7109871A
PADDING = 0x42726577


def lp(data, offset=0):
    if offset + 4 > len(data):
        raise ValueError("Truncated signature length")
    size = struct.unpack_from("<I", data, offset)[0]
    end = offset + 4 + size
    if end > len(data):
        raise ValueError("Truncated signature value")
    return data[offset + 4:end], end


def check_manager_signature(apk):
    data = Path(apk).read_bytes()
    eocd = data.rfind(b"PK\x05\x06", max(0, len(data) - 65557))
    if eocd < 0 or eocd + 22 > len(data):
        raise ValueError("Missing ZIP footer")
    if eocd + 22 + struct.unpack_from("<H", data, eocd + 20)[0] != len(data):
        raise ValueError("Invalid ZIP footer")
    cd = struct.unpack_from("<I", data, eocd + 16)[0]
    if cd < 32 or data[cd - 16:cd] != b"APK Sig Block 42":
        raise ValueError("Missing APK signing block")
    size = struct.unpack_from("<Q", data, cd - 24)[0]
    start = cd - size - 8
    if start < 0 or size < 24 or struct.unpack_from("<Q", data, start)[0] != size:
        raise ValueError("Invalid APK signing block")
    offset, end = start + 8, cd - 24
    blocks = []
    while offset < end:
        if offset + 12 > end:
            raise ValueError("Truncated APK signing pair")
        length, block_id = struct.unpack_from("<QI", data, offset)
        next_offset = offset + 8 + length
        if length < 4 or next_offset > end:
            raise ValueError("Invalid APK signing pair")
        blocks.append((block_id, data[offset + 12:next_offset]))
        offset = next_offset
    if any(key not in (V2, PADDING) for key, _ in blocks):
        raise ValueError("KernelSU manager requires V2 only; V3/V3.1/other blocks are rejected")
    v2 = [value for key, value in blocks if key == V2]
    if len(v2) != 1:
        raise ValueError("Exactly one V2 signing block is required")
    signers, consumed = lp(v2[0])
    signer, signer_end = lp(signers)
    if consumed != len(v2[0]) or signer_end != len(signers):
        raise ValueError("Exactly one APK signer is required")
    signed_data, _ = lp(signer)
    _, after_digests = lp(signed_data)
    certificates, _ = lp(signed_data, after_digests)
    certificate, cert_end = lp(certificates)
    if cert_end != len(certificates) or not 256 <= len(certificate) <= 1024:
        raise ValueError("Unsupported manager signing certificate")
    with zipfile.ZipFile(apk) as bundle:
        if "META-INF/MANIFEST.MF" in bundle.namelist():
            raise ValueError("KernelSU manager rejects V1/JAR signatures")
    return len(certificate), hashlib.sha256(certificate).hexdigest()
