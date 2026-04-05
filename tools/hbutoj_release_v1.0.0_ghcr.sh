#!/usr/bin/env bash
set -euo pipefail

OWNER="1650041940"
TAG="v1.0.0"
DEBUG=0

IMAGES=(
  "ghcr.io/${OWNER}/hbutoj_backend:${TAG}"
  "ghcr.io/${OWNER}/hbutoj_frontend:${TAG}"
  "ghcr.io/${OWNER}/hbutoj_judgeserver:${TAG}"
)

PACKAGES=(
  "hbutoj_backend"
  "hbutoj_frontend"
  "hbutoj_judgeserver"
)

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "Missing command: $1" >&2; exit 1; }
}

require_cmd docker
require_cmd gh

API_VERSION_HEADER="X-GitHub-Api-Version: 2022-11-28"
ACCEPT_HEADER="Accept: application/vnd.github+json"

usage() {
  echo "Usage: $0 [--stdin] [--set-public-only] [--debug]" >&2
  echo "  --stdin            Read GitHub PAT from STDIN (single line)." >&2
  echo "  --set-public-only  Only set GHCR packages visibility to public (skip docker push)." >&2
  echo "  --debug            Print API attempts and response headers (no token shown)." >&2
}

set_public_visibility() {
  local package_name="$1"
  local paths=(
    "/user/packages/container/${package_name}/visibility"
    "/user/packages/container/${package_name}"
    "/users/${OWNER}/packages/container/${package_name}/visibility"
    "/users/${OWNER}/packages/container/${package_name}"
  )

  for p in "${paths[@]}"; do
    # Try both PATCH and PUT; some GitHub endpoints accept only one method.
    if [[ "$DEBUG" -eq 1 ]]; then
      echo "[debug] PATCH ${p} visibility=public" >&2
      if gh api -i --method PATCH -H "$ACCEPT_HEADER" -H "$API_VERSION_HEADER" "$p" -f visibility=public 2>&1 | sed -n '1,30p' >&2; then
        echo "- ${package_name}: public (PATCH ${p})"
        return 0
      fi

      echo "[debug] PUT ${p} visibility=public" >&2
      if gh api -i --method PUT -H "$ACCEPT_HEADER" -H "$API_VERSION_HEADER" "$p" -f visibility=public 2>&1 | sed -n '1,30p' >&2; then
        echo "- ${package_name}: public (PUT ${p})"
        return 0
      fi
    else
      if gh api --method PATCH -H "$ACCEPT_HEADER" -H "$API_VERSION_HEADER" "$p" -f visibility=public >/dev/null 2>&1; then
        echo "- ${package_name}: public (PATCH ${p})"
        return 0
      fi
      if gh api --method PUT -H "$ACCEPT_HEADER" -H "$API_VERSION_HEADER" "$p" -f visibility=public >/dev/null 2>&1; then
        echo "- ${package_name}: public (PUT ${p})"
        return 0
      fi
    fi
  done

  return 1
}

print_package_visibility_and_url() {
  local package_name="$1"
  gh api \
    -H "$ACCEPT_HEADER" \
    -H "$API_VERSION_HEADER" \
    "/user/packages/container/${package_name}" \
    --jq '"- " + .name + ": visibility=" + .visibility + " url=" + .html_url' \
    2>/dev/null \
  || echo "- ${package_name}: (failed to fetch metadata)" >&2
}

list_my_container_packages() {
  gh api \
    -H "$ACCEPT_HEADER" \
    -H "$API_VERSION_HEADER" \
    "/user/packages?package_type=container&per_page=100" \
    --jq '.[].name' \
    2>/dev/null \
  || true
}

debug_whoami_and_scopes() {
  echo "---- gh api -i /user (shows login + OAuth scopes) ----" >&2
  gh api -i -H "$ACCEPT_HEADER" -H "$API_VERSION_HEADER" /user 2>&1 | sed -n '1,120p' >&2 || true
  echo "-----------------------------------------------------" >&2
}

GHCR_TOKEN_VALUE="${GHCR_TOKEN-}"
SET_PUBLIC_ONLY=0
READ_TOKEN_FROM_STDIN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --set-public-only)
      SET_PUBLIC_ONLY=1
      shift
      ;;
    --stdin)
      READ_TOKEN_FROM_STDIN=1
      shift
      ;;
    --debug)
      DEBUG=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

if [[ "$SET_PUBLIC_ONLY" -eq 0 ]]; then
  if ! docker image inspect "${IMAGES[0]}" >/dev/null 2>&1; then
    echo "Missing local image: ${IMAGES[0]}" >&2
    exit 1
  fi
  if ! docker image inspect "${IMAGES[1]}" >/dev/null 2>&1; then
    echo "Missing local image: ${IMAGES[1]}" >&2
    exit 1
  fi
  if ! docker image inspect "${IMAGES[2]}" >/dev/null 2>&1; then
    echo "Missing local image: ${IMAGES[2]}" >&2
    exit 1
  fi
fi

echo "This script will:"
echo "- docker login ghcr.io as ${OWNER} (token from prompt/env/stdin)"
if [[ "$SET_PUBLIC_ONLY" -eq 0 ]]; then
  echo "- docker push 3 images to GHCR with tag ${TAG}"
fi
echo "- set the 3 GHCR packages to public via GitHub API"
echo

if [[ "$READ_TOKEN_FROM_STDIN" -eq 1 ]]; then
  # Read a single line token from STDIN
  read -r GHCR_TOKEN_VALUE
elif [[ -z "$GHCR_TOKEN_VALUE" ]]; then
  echo "Enter a fresh GitHub PAT (classic) with: write:packages, read:packages"
  echo "(Input is hidden; it is NOT echoed. Docker will store a GHCR login for pushing.)"
  read -r -s GHCR_TOKEN_VALUE
  echo
fi

if [[ -z "$GHCR_TOKEN_VALUE" ]]; then
  echo "Empty token. Aborting." >&2
  exit 1
fi

# Login to GHCR
printf '%s' "$GHCR_TOKEN_VALUE" | docker login ghcr.io -u "$OWNER" --password-stdin >/dev/null

# Use token only for this process for GitHub API calls (do not persist gh auth)
export GH_TOKEN="$GHCR_TOKEN_VALUE"

unset GHCR_TOKEN_VALUE

if [[ "$SET_PUBLIC_ONLY" -eq 0 ]]; then
  echo "Pushing images..."
  for img in "${IMAGES[@]}"; do
    echo "- $img"
    docker push "$img"
  done
fi

echo "Setting packages to public..."
failed=0
for pkg in "${PACKAGES[@]}"; do
  # Some GHCR package names may normalize '_' to '-'. Try both.
  if set_public_visibility "$pkg"; then
    continue
  fi
  alt_pkg="${pkg//_/-}"
  if [[ "$alt_pkg" != "$pkg" ]] && set_public_visibility "$alt_pkg"; then
    continue
  fi

  echo "- ${pkg}: failed to set public" >&2
  failed=1
done

if [[ "$failed" -ne 0 ]]; then
  echo >&2
  echo "Visibility update failed." >&2
  echo "- If the API returns HTTP 404: it can mean wrong package name/owner, missing permissions/scopes (sometimes masked as 404)," >&2
  echo "  OR GitHub may not currently expose a REST endpoint for changing package visibility." >&2
  echo >&2
  echo "Fastest reliable workaround: set visibility to Public in GitHub UI (irreversible)." >&2
  echo "- Your profile -> Packages -> select the container package -> Package settings" >&2
  echo "- Danger Zone -> Change visibility -> Public" >&2
  echo >&2
  echo "Package URLs + current visibility:" >&2
  for pkg in "${PACKAGES[@]}"; do
    print_package_visibility_and_url "$pkg" >&2 || true
  done
  echo >&2
  echo "Packages visible to current token (container):" >&2
  list_my_container_packages | sed 's/^/  - /' >&2
  echo >&2
  debug_whoami_and_scopes
  exit 1
fi

unset GH_TOKEN

echo "Done."
