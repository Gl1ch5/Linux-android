#!/usr/bin/env bash
# Builds Turnip (Mesa freedreno Vulkan, KGSL kernel backend) for aarch64.
#
#   usage: build-turnip.sh <channel:stable|nightly> <libc:glibc|musl> <out-dir>
#
# Output in <out-dir>:
#   turnip-<channel>-<libc>-<version>.tar.zst   driver archive (lib + ICD json)
#   turnip-<channel>-<libc>-<version>.tar.zst.sha256
#   manifest-<channel>-<libc>.json              version, commit, date, channel, libc
#
# Run on an x86_64 Linux host (glibc build) or inside an aarch64 Alpine container (musl build).
set -euo pipefail

CHANNEL="${1:?channel: stable|nightly}"
LIBC="${2:?libc: glibc|musl}"
OUT="${3:?output directory}"
MESA_API="https://gitlab.freedesktop.org/api/v4/projects/mesa%2Fmesa"
WORK="${WORK:-$PWD/work-$CHANNEL-$LIBC}"
JOBS="$(nproc)"

mkdir -p "$WORK" "$OUT"
cd "$WORK"

# --- Pick the source revision -------------------------------------------------
if [[ "$CHANNEL" == "stable" ]]; then
  # Latest release tag: mesa-X.Y.Z (no rc suffix).
  REF="$(curl -fsSL "$MESA_API/repository/tags?order_by=version&sort=desc&per_page=50" \
    | python3 -c 'import json,sys,re; t=[x["name"] for x in json.load(sys.stdin) if re.fullmatch(r"mesa-\d+\.\d+\.\d+", x["name"])]; print(t[0])')"
else
  REF="main"
fi
echo "Mesa ref: $REF"

if [[ ! -d mesa/.git ]]; then
  git clone --depth 1 --branch "$REF" https://gitlab.freedesktop.org/mesa/mesa.git mesa
else
  git -C mesa fetch --depth 1 origin "$REF" && git -C mesa checkout -f FETCH_HEAD
fi
COMMIT="$(git -C mesa rev-parse HEAD)"
VERSION="$(python3 -c 'import re,sys; print(re.search(r"version:\s*\x27([^\x27]+)\x27", open(sys.argv[1]).read()).group(1))' mesa/meson.build)"
[[ "$CHANNEL" == "nightly" ]] && VERSION="$VERSION-git${COMMIT::8}"

# --- Toolchain ----------------------------------------------------------------
# Meson cross file. glibc: Debian gcc cross. musl: native gcc inside the aarch64 Alpine container.
if [[ "$LIBC" == "glibc" ]]; then
  CC=aarch64-linux-gnu-gcc
  CXX=aarch64-linux-gnu-g++
  STRIP=aarch64-linux-gnu-strip
  CPU_FAMILY=aarch64
else
  CC=gcc
  CXX=g++
  STRIP=strip
  CPU_FAMILY=aarch64
fi

cat > cross.ini <<EOF
[binaries]
c = '$CC'
cpp = '$CXX'
ar = 'ar'
strip = '$STRIP'
pkgconfig = 'pkg-config'

[host_machine]
system = 'linux'
cpu_family = '$CPU_FAMILY'
cpu = 'aarch64'
endian = 'little'
EOF

# --- Build --------------------------------------------------------------------
# Only the Vulkan freedreno driver with the KGSL kmd. Everything else is off to keep the build small.
meson setup build-$LIBC mesa \
  --cross-file cross.ini \
  --buildtype=release \
  -Dplatforms= \
  -Dgallium-drivers= \
  -Dvulkan-drivers=freedreno \
  -Dfreedreno-kmds=kgsl \
  -Dglx=disabled \
  -Degl=disabled \
  -Dgles1=disabled -Dgles2=disabled \
  -Dllvm=disabled \
  -Dvalgrind=disabled \
  -Dlibunwind=disabled \
  -Dbuild-tests=false
ninja -C build-$LIBC -j"$JOBS"

# --- Package ------------------------------------------------------------------
STAGE="$WORK/stage"
rm -rf "$STAGE" && mkdir -p "$STAGE/lib" "$STAGE/share/vulkan/icd.d"
DRIVER="$(find build-$LIBC/src/freedreno/vulkan -name 'libvulkan_freedreno.so' | head -1)"
cp "$DRIVER" "$STAGE/lib/libvulkan_freedreno.so"
cat > "$STAGE/share/vulkan/icd.d/freedreno_icd.aarch64.json" <<'JSON'
{
    "file_format_version": "1.0.0",
    "ICD": {
        "library_path": "/usr/lib/libvulkan_freedreno.so",
        "api_version": "1.3.0"
    }
}
JSON
cat > "$STAGE/build-info.txt" <<EOF
mesa_ref=$REF
mesa_commit=$COMMIT
version=$VERSION
channel=$CHANNEL
libc=$LIBC
EOF

ARCHIVE="turnip-$CHANNEL-$LIBC-$VERSION.tar.zst"
tar -C "$STAGE" -cf - . | zstd -19 -T0 -o "$OUT/$ARCHIVE" -f
(cd "$OUT" && sha256sum "$ARCHIVE" > "$ARCHIVE.sha256")

SHA="$(cut -d' ' -f1 "$OUT/$ARCHIVE.sha256")"
cat > "$OUT/manifest-$CHANNEL-$LIBC.json" <<EOF
{
  "name": "turnip",
  "version": "$VERSION",
  "mesa_ref": "$REF",
  "mesa_commit": "$COMMIT",
  "channel": "$CHANNEL",
  "libc": "$LIBC",
  "kmd": "kgsl",
  "arch": "aarch64",
  "file": "$ARCHIVE",
  "sha256": "$SHA",
  "built_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
echo "built $ARCHIVE sha256=$SHA"
