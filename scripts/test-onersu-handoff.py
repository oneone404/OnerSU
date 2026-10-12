#!/usr/bin/env python3
"""Exercise the real bootstrap with mock ksud/am, without an Android device."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parents[1]
SH = os.environ.get("ONERSU_TEST_SH") or shutil.which("sh")
if not SH:
    raise SystemExit("Set ONERSU_TEST_SH to a POSIX sh executable")


def quote(value):
    return "'" + str(value).replace("'", "'\"'\"'") + "'"


def wait_for(predicate, seconds=5):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        if predicate():
            return
        time.sleep(0.05)
    raise AssertionError("Timed out waiting for mocked handoff")


with tempfile.TemporaryDirectory(prefix="onersu-handoff-") as tmp:
    folder = Path(tmp)
    ready, calls = folder / "ready", folder / "calls"
    ksud, apk, am = folder / "ksud", folder / "app.apk", folder / "am"
    ksud.write_text(
        '#!/bin/sh\n'
        'printf "%s\\n" "$*" >> "$TEST_CALLS"\n'
        '[ "$1" = late-load ] && exit "${TEST_LATE_FAIL:-0}"\n'
        '[ -f "$TEST_DRIVER_READY" ]\n', encoding="utf-8", newline="\n")
    am.write_text('#!/bin/sh\nprintf "am %s\\n" "$*" >> "$TEST_CALLS"\n',
                  encoding="utf-8", newline="\n")
    sleeper = folder / "sleep"
    sleeper.write_text('#!/bin/sh\n"$TEST_PYTHON" -c "import time; time.sleep(float(__import__(\'sys\').argv[1]))" "$1"\n',
                       encoding="utf-8", newline="\n")
    apk.write_bytes(b"mock APK")
    script = (ROOT / "manager/app/src/main/assets/ksud").read_text()
    assert "PATH=/system/bin:/system/xbin" in script
    # This PC fixture supplies mock commands instead of Android's system tools.
    script = script.replace("PATH=/system/bin:/system/xbin", "# Use fixture PATH")
    for key, value in {"KSUD": ksud.as_posix(), "APK": apk.as_posix(),
                       "READY": ready.as_posix(), "RUN_ID": "fresh-run"}.items():
        script = script.replace("@" + key + "@", quote(value))
    launcher = folder / "bootstrap"
    launcher.write_text(script, encoding="utf-8", newline="\n")
    env = os.environ.copy()
    env.update(TEST_CALLS=calls.as_posix(), TEST_DRIVER_READY=(folder / "driver-ready").as_posix())
    env["TEST_PYTHON"] = Path(sys.executable).as_posix()
    env["PATH"] = folder.as_posix() + os.pathsep + str(Path(SH).parent) + os.pathsep + env["PATH"]
    for executable in (ksud, am, sleeper):
        executable.chmod(0o755)
    ready.write_text("old-run")
    process = subprocess.Popen([SH, launcher.as_posix()], env=env)
    wait_for(lambda: calls.exists())
    time.sleep(0.2)
    assert process.poll() is None, "Must wait until driver registration succeeds"
    (folder / "driver-ready").write_text("ready")
    assert process.wait(timeout=5) == 0
    time.sleep(0.25)
    assert "am " not in calls.read_text(), "A stale cleanup token must not restart the app"
    ready.write_text("fresh-run")
    wait_for(lambda: "am start" in calls.read_text())
    text = calls.read_text()
    assert "late-load --kmi android16-6.12 --package-name com.onersu.onersu.bootstrap" in text
    assert "am force-stop com.onersu.onersu\nam start -n com.onersu.onersu/org.bakasu.bakasu.ui.MainActivity" in text
    calls.write_text("")
    env["TEST_LATE_FAIL"] = "1"
    assert subprocess.run([SH, launcher.as_posix()], env=env).returncode == 1
    assert "am " not in calls.read_text(), "Failed late-load must not restart"
    print("PASS: waits for backend, rejects stale token, restarts after cleanup, stops on failure")
