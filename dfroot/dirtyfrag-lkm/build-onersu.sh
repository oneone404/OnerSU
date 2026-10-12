#!/bin/sh
set -eu

# OnerSU ships only the PME110 android16-6.12 profile.
ENGINE="${ENGINE:-docker}"
IMAGE=ghcr.io/ylarod/ddk-min@sha256:fd3ec5cc71d7f2716212e9df8d5674a9afe68fb769282fcac87b83f207f949ee
if [ -n "${ANDROID_NDK_ROOT:-}" ]; then
    LLVM_OBJCOPY="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objcopy"
else
    LLVM_OBJCOPY=llvm-objcopy
fi

cd "$(dirname "$0")"
"$ENGINE" run --rm --pid=host --network=none -v "$(pwd)":/src -w /src "$IMAGE" make "$@"
"$LLVM_OBJCOPY" --strip-unneeded \
    -R .comment -R .note.gnu.build-id -R .note.gnu.property -R .note.Linux -R .note.GNU-stack \
    -R .BTF -R .BTF.base -R .llvm_addrsig \
    -R .hyp.text -R .hyp.bss -R .hyp.rodata -R .hyp.event_ids \
    -R .hyp.patchable_function_entries -R .hyp.data \
    dirtyfrag.ko ../../manager/app/src/main/cpp/dfroot/ko/dirtyfrag-android16-6.12.ko
echo 'Built OnerSU bootstrap for android16-6.12'
