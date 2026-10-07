#!/usr/bin/env bash
set -euo pipefail

registry="https://npm.pkg.github.com"
packages=(
  "@jhfmiguel/faria-miguel-platform@0.1.0"
  "@jhfmiguel/faria-miguel-ui@0.1.0"
)

missing=()

for package in "${packages[@]}"; do
  if ! npm view "$package" version --registry="$registry" >/dev/null 2>&1; then
    missing+=("$package")
  fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "Canonical Faria Miguel frontend packages are not fully available from GitHub Packages." >&2
  echo "Missing/unavailable packages:" >&2
  for package in "${missing[@]}"; do
    echo " - $package" >&2
  done
  exit 1
fi

echo "Canonical Faria Miguel frontend packages are available."
