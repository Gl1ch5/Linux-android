#!/usr/bin/env bash
# Builds the lightweight XFCE rootfs (Debian trixie, aarch64) as rootfs.tar.zst.
#
#   usage: build-rootfs-xfce.sh <out-dir>
#
# Must run as root on an aarch64 Linux host (or inside a privileged aarch64 container).
# Output in <out-dir>:
#   xfce-light-<date>.rootfs.tar.zst, .sha256, and a fragment image-xfce-light.json
set -euo pipefail

OUT="${1:?output directory}"
SUITE=trixie
DATE="$(date -u +%Y%m%d)"
NAME="xfce-light-$DATE"
ROOTFS="$(mktemp -d)/rootfs"
mkdir -p "$OUT"

# --- Base system: minbase keeps the image small ----------------------------------
debootstrap --variant=minbase --arch=arm64 "$SUITE" "$ROOTFS" http://deb.debian.org/debian

# --- Desktop and tools ------------------------------------------------------------
# --no-install-recommends is essential here: recommends pull in several hundred MB.
cat > "$ROOTFS/tmp/install.sh" <<'EOF'
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends \
  xfce4 xfce4-terminal xfce4-session dbus-x11 \
  mesa-utils vulkan-tools \
  fonts-dejavu-core locales sudo
apt-get clean
rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*.deb
EOF
chroot "$ROOTFS" /bin/bash /tmp/install.sh
rm -f "$ROOTFS/tmp/install.sh"

# --- Defaults ----------------------------------------------------------------------
echo "linuxdesk" > "$ROOTFS/etc/hostname"
printf 'nameserver 1.1.1.1\n' > "$ROOTFS/etc/resolv.conf"
mkdir -p "$ROOTFS/root"

# --- Pack ---------------------------------------------------------------------------
ARCHIVE="$OUT/$NAME.rootfs.tar.zst"
tar -C "$ROOTFS" --numeric-owner -cf - . | zstd -19 -T0 -o "$ARCHIVE" -f
SHA="$(sha256sum "$ARCHIVE" | cut -d' ' -f1)"
echo "$SHA  $(basename "$ARCHIVE")" > "$ARCHIVE.sha256"

SIZE_PACKED="$(stat -c%s "$ARCHIVE")"
SIZE_UNPACKED="$(du -sb "$ROOTFS" | cut -f1)"
cat > "$OUT/image-xfce-light.json" <<EOF
{
  "id": "xfce-light",
  "name": "XFCE (лёгкий)",
  "description": "Debian trixie minimal + XFCE",
  "version": "$DATE",
  "session": "x11",
  "sha256": "$SHA",
  "size_archive": $SIZE_PACKED,
  "size_unpacked": $SIZE_UNPACKED,
  "file": "$(basename "$ARCHIVE")",
  "run": ["/usr/bin/startxfce4"],
  "preview": null
}
EOF
echo "built $(basename "$ARCHIVE") sha256=$SHA"
