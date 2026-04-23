#!/bin/bash
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

FLY_HOME="${FLY_HOME:-$HOME/.fly}"
FLY_BIN="$FLY_HOME/bin/flyctl"

install_flyctl() {
  mkdir -p "$FLY_HOME/bin"

  local arch
  case "$(uname -m)" in
    x86_64|amd64) arch="x86_64" ;;
    aarch64|arm64) arch="arm64" ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; return 1 ;;
  esac

  local version
  version="$(curl -fsSL https://api.github.com/repos/superfly/flyctl/releases/latest \
    | grep -oE '"tag_name": *"v[^"]+"' | head -1 | sed -E 's/.*"v([^"]+)".*/\1/')"
  if [ -z "$version" ]; then
    echo "Failed to determine latest flyctl version" >&2
    return 1
  fi

  local url="https://github.com/superfly/flyctl/releases/download/v${version}/flyctl_${version}_Linux_${arch}.tar.gz"
  local tmp
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN

  curl -fsSL "$url" -o "$tmp/flyctl.tar.gz"
  tar -xzf "$tmp/flyctl.tar.gz" -C "$FLY_HOME/bin" flyctl
  chmod +x "$FLY_HOME/bin/flyctl"
}

if [ ! -x "$FLY_BIN" ]; then
  install_flyctl
fi

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export FLYCTL_INSTALL=\"$FLY_HOME\"" >> "$CLAUDE_ENV_FILE"
  echo "export PATH=\"$FLY_HOME/bin:\$PATH\"" >> "$CLAUDE_ENV_FILE"
fi

"$FLY_BIN" version
