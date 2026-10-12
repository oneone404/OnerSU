#!/system/bin/sh
# OnerSU's bounded boot policy. Only the OPPO installer and InstallStart are changed.
PATH=/system/bin:/system/xbin
BB=/data/adb/ksu/bin/busybox
STATE=/data/adb/onersu/installer-guard
SCRIPT=$STATE/guard.sh
HOOK=/data/adb/boot-completed.d/onersu-installer-guard.sh
PKG=com.android.packageinstaller
COMP=$PKG/$PKG.InstallStart

[ "$(id -u)" = 0 ] && [ -x "$BB" ] || exit 1
umask 077
mkdir -p "$STATE" || exit 1

# Android's mksh closes extra descriptors on exec. Let BusyBox own the lock and
# launch the operation instead of passing a numeric descriptor across shells.
if [ "$1" != boot ] && [ "$2" != locked ]; then
    exec "$BB" flock -x "$STATE/lock" "$BB" sh "$SCRIPT" "$1" locked
fi

# The package dump is read once per pass; no package names or commands come from it.
rows() {
    /system/bin/dumpsys package "$PKG" | "$BB" awk '
        function flush() { if (active) print uid, installed, app, component }
        /^    User [0-9]+: .*installed=/ {
            flush(); active=1; uid=$2; sub(/:$/, "", uid); component=0; section=0
            for (i=1;i<=NF;i++) {
                if ($i ~ /^installed=/) { installed=$i; sub(/^installed=/, "", installed) }
                if ($i ~ /^enabled=/) { app=$i; sub(/^enabled=/, "", app) }
            }
            next
        }
        /^    [^ ]/ { flush(); active=0; section=0 }
        /^      enabledComponents:/ { section=1; next }
        /^      disabledComponents:/ { section=2; next }
        /^      [^ ]/ { section=0 }
        active && section && $1 == "com.android.packageinstaller.InstallStart" { component=section }
        END { flush() }
    '
}

current_user() {
    target=$(/system/bin/am get-current-user </dev/null) || return 1
    case "$target" in ''|*[!0-9]*) return 1;; esac
    printf '%s\n' "$target"
}

valid_row() {
    case "$1" in ''|*[!0-9]*) return 1;; esac
    case "$2" in true|false) ;; *) return 1;; esac
    case "$3" in 0|1|2|3|4) ;; *) return 1;; esac
    case "$4" in 0|1|2) ;; *) return 1;; esac
}

run_pm() {
    # Binder's package service cannot write directly to a private /data/adb file.
    # Capture through a pipe, then let this root process append to the log.
    output=$("$BB" timeout 10 /system/bin/pm "$@" </dev/null 2>&1)
    result=$?
    printf '%s\n' "$output" >>"$STATE/last.log"
    return "$result"
}

verify_block() {
    target=$(current_user) || return 1
    [ -f "$STATE/before.tsv" ] || return 1
    "$BB" grep -q "^$target " "$STATE/before.tsv" || return 1
    # A policy previously applied to another user must first be restored.
    "$BB" grep -qv "^$target " "$STATE/before.tsv" && return 1
    current=$(rows | "$BB" grep "^$target ")
    [ -n "$current" ] || return 1
    while read -r uid installed app component; do
        valid_row "$uid" "$installed" "$app" "$component" || return 1
        [ "$app" = 3 ] && [ "$component" = 2 ] || return 1
    done <<EOF
$current
EOF
}

apply() {
    [ -f "$STATE/enabled" ] || return 0
    target=$(current_user) || return 1
    if [ -f "$STATE/before.tsv" ] && "$BB" grep -qv "^$target " "$STATE/before.tsv"; then
        restore || return 1
    fi
    current=$(rows | "$BB" grep "^$target ")
    [ -n "$current" ] || return 1
    failed=0
    while read -r uid installed app component; do
        valid_row "$uid" "$installed" "$app" "$component" || return 1
        # Snapshot the current user before its first mutation in this enabled session.
        if ! "$BB" grep -q "^$uid " "$STATE/before.tsv" 2>/dev/null; then
            printf '%s %s %s %s\n' "$uid" "$installed" "$app" "$component" >>"$STATE/before.tsv" || return 1
        fi
        run_pm disable-user --user "$uid" "$PKG" || failed=1
        run_pm disable --user "$uid" "$COMP" || failed=1
    done <<EOF
$current
EOF
    verify_block || failed=1
    [ "$failed" = 0 ]
}

set_state() {
    case "$3" in
        0) operation=default-state;;
        1) operation=enable;;
        2) operation=disable;;
        3) operation=disable-user;;
        4) operation=disable-until-used;;
        *) return 1;;
    esac
    run_pm "$operation" --user "$1" "$2"
}

restore() {
    [ -f "$STATE/before.tsv" ] || return 0
    # Deleted users require no restoration. Preserve every surviving user's baseline.
    current=$(rows)
    [ -n "$current" ] || return 1
    failed=0
    while read -r uid installed app component; do
        valid_row "$uid" "$installed" "$app" "$component" || return 1
        printf '%s\n' "$current" | "$BB" grep -q "^$uid " || continue
        if [ "$installed" = true ]; then
            run_pm install-existing --user "$uid" "$PKG" || failed=1
        elif ! printf '%s\n' "$current" | "$BB" grep -q "^$uid false "; then
            run_pm uninstall --user "$uid" "$PKG" || failed=1
        fi
        set_state "$uid" "$COMP" "$component" || failed=1
        set_state "$uid" "$PKG" "$app" || failed=1
    done <"$STATE/before.tsv"
    current=$(rows)
    while read -r uid installed app component; do
        valid_row "$uid" "$installed" "$app" "$component" || return 1
        actual=$(printf '%s\n' "$current" | "$BB" grep "^$uid ")
        [ -z "$actual" ] || [ "$actual" = "$uid $installed $app $component" ] || failed=1
    done <"$STATE/before.tsv"
    [ "$failed" = 0 ] || return 1
    rm -f "$STATE/before.tsv"
}

case "$1" in
    boot)
        # No permanent worker: retry at boot completion, +5s and +25s, then exit.
        for delay in 0 5 20; do
            [ "$delay" = 0 ] || sleep "$delay"
            [ -f "$STATE/enabled" ] || exit 0
            sh "$SCRIPT" apply || :
        done
        exit 0
        ;;
    enable)
        : >"$STATE/last.log"
        mkdir -p /data/adb/boot-completed.d || exit 1
        printf '#!/system/bin/sh\nexec /system/bin/sh %s boot\n' "$SCRIPT" >"$HOOK.tmp" || exit 1
        chmod 700 "$HOOK.tmp" && mv -f "$HOOK.tmp" "$HOOK" || exit 1
        touch "$STATE/enabled" || exit 1
        apply || exit 1
        echo blocked
        ;;
    apply)
        : >"$STATE/last.log"
        apply || exit 1
        ;;
    disable)
        : >"$STATE/last.log"
        rm -f "$STATE/enabled" "$HOOK" "$HOOK.tmp"
        restore || exit 1
        echo off
        ;;
    status)
        if [ -f "$STATE/enabled" ]; then
            verify_block || exit 1
            echo blocked
        elif [ -f "$STATE/before.tsv" ]; then
            exit 1
        else
            echo off
        fi
        ;;
    *) exit 2;;
esac
