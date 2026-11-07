#!/bin/sh
# Reuse an existing Maven installation; never download or install Maven.
set -eu
if [ -n "${MAVEN_BIN:-}" ]; then
  exec "$MAVEN_BIN" "$@"
fi
for candidate in "$HOME/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn" "/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn"; do
  if [ -x "$candidate" ]; then exec "$candidate" "$@"; fi
done
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
printf '%s\n' 'Set MAVEN_BIN to your IDE bundled Maven executable.' >&2
exit 1
