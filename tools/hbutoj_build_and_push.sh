#!/usr/bin/env bash
set -euo pipefail

# Build + push images used by /root/services/hbutoj_deplay.
#
# Usage:
#   export HBUTOJ_IMAGE_PREFIX=ghcr.io/<you>
#   export HBUTOJ_IMAGE_TAG=v1.0.0
#   # optional overrides
#   export HBUTOJ_BACKEND_IMAGE=hbutoj_backend
#   export HBUTOJ_FRONTEND_IMAGE=hbutoj_frontend
#   export HBUTOJ_JUDGESERVER_IMAGE=hbutoj_judgeserver
#   export HBUTOJ_MYSQL_CHECKER_IMAGE=hbutoj_database_checker
#   # optional: also build & push mysql image
#   export HBUTOJ_BUILD_MYSQL=true
#   export HBUTOJ_MYSQL_IMAGE=hbutoj_database
#   export DEPLOY_REPO=/root/services/hbutoj_deplay
#   ./tools/hbutoj_build_and_push.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_REPO="${DEPLOY_REPO:-/root/services/hbutoj_deplay}"

load_env_defaults() {
  local env_file="$1"
  [[ -f "$env_file" ]] || return 0

  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ "$line" =~ ^[[:space:]]*$ ]] && continue

    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      local key="${BASH_REMATCH[1]}"
      local val="${BASH_REMATCH[2]}"

      # Do not override an already-provided environment variable.
      if [[ -z "${!key-}" ]]; then
        export "$key=$val"
      fi
    fi
  done < "$env_file"
}

ENV_FILE=""
if [[ -f "$DEPLOY_REPO/standAlone/.env" ]]; then
  ENV_FILE="$DEPLOY_REPO/standAlone/.env"
elif [[ -f "$DEPLOY_REPO/distributed/main/.env" ]]; then
  ENV_FILE="$DEPLOY_REPO/distributed/main/.env"
fi

if [[ -n "$ENV_FILE" ]]; then
  echo "==> Loading defaults from: $ENV_FILE"
  load_env_defaults "$ENV_FILE"
fi

HBUTOJ_IMAGE_PREFIX="${HBUTOJ_IMAGE_PREFIX:-}"
HBUTOJ_IMAGE_TAG="${HBUTOJ_IMAGE_TAG:-}"
HBUTOJ_PUSH="${HBUTOJ_PUSH:-true}"
HBUTOJ_BUILD_MYSQL="${HBUTOJ_BUILD_MYSQL:-false}"
HBUTOJ_BUILD_MYSQL_CHECKER="${HBUTOJ_BUILD_MYSQL_CHECKER:-true}"
HBUTOJ_MVN_QUIET="${HBUTOJ_MVN_QUIET:-true}"
HBUTOJ_NPM_REGISTRY="${HBUTOJ_NPM_REGISTRY:-https://registry.npmjs.org/}"

HBUTOJ_MYSQL_IMAGE_PREFIX="${HBUTOJ_MYSQL_IMAGE_PREFIX:-${HBUTOJ_IMAGE_PREFIX:-}}"
HBUTOJ_MYSQL_CHECKER_IMAGE_PREFIX="${HBUTOJ_MYSQL_CHECKER_IMAGE_PREFIX:-${HBUTOJ_IMAGE_PREFIX:-}}"
HBUTOJ_BACKEND_IMAGE_TAG="${HBUTOJ_BACKEND_IMAGE_TAG:-}"
HBUTOJ_FRONTEND_IMAGE_TAG="${HBUTOJ_FRONTEND_IMAGE_TAG:-}"
HBUTOJ_JUDGESERVER_IMAGE_TAG="${HBUTOJ_JUDGESERVER_IMAGE_TAG:-}"
HBUTOJ_MYSQL_IMAGE_TAG="${HBUTOJ_MYSQL_IMAGE_TAG:-}"
HBUTOJ_MYSQL_CHECKER_IMAGE_TAG="${HBUTOJ_MYSQL_CHECKER_IMAGE_TAG:-}"

HBUTOJ_MYSQL_IMAGE="${HBUTOJ_MYSQL_IMAGE:-hbutoj_database}"
HBUTOJ_BACKEND_IMAGE="${HBUTOJ_BACKEND_IMAGE:-hbutoj_backend}"
HBUTOJ_FRONTEND_IMAGE="${HBUTOJ_FRONTEND_IMAGE:-hbutoj_frontend}"
HBUTOJ_JUDGESERVER_IMAGE="${HBUTOJ_JUDGESERVER_IMAGE:-hbutoj_judgeserver}"
HBUTOJ_MYSQL_CHECKER_IMAGE="${HBUTOJ_MYSQL_CHECKER_IMAGE:-hbutoj_database_checker}"

validate_no_placeholder() {
  local name="$1"
  local value="$2"
  if [[ "$value" == *"<"* || "$value" == *">"* ]]; then
    echo "ERROR: $name contains a placeholder: $value" >&2
    echo "       Please replace it with a real registry namespace, e.g.:" >&2
    echo "         export HBUTOJ_IMAGE_PREFIX=ghcr.io/<your_github_username>" >&2
    exit 1
  fi
}

if [[ -z "$HBUTOJ_IMAGE_PREFIX" ]]; then
  echo "ERROR: HBUTOJ_IMAGE_PREFIX is required (e.g. ghcr.io/<you> or registry.cn-shenzhen.aliyuncs.com/<namespace>)." >&2
  exit 1
fi

validate_no_placeholder HBUTOJ_IMAGE_PREFIX "$HBUTOJ_IMAGE_PREFIX"
validate_no_placeholder HBUTOJ_MYSQL_IMAGE_PREFIX "${HBUTOJ_MYSQL_IMAGE_PREFIX:-}"
validate_no_placeholder HBUTOJ_MYSQL_CHECKER_IMAGE_PREFIX "${HBUTOJ_MYSQL_CHECKER_IMAGE_PREFIX:-}"

if [[ -z "$HBUTOJ_IMAGE_TAG" && -z "$HBUTOJ_BACKEND_IMAGE_TAG" && -z "$HBUTOJ_FRONTEND_IMAGE_TAG" && -z "$HBUTOJ_JUDGESERVER_IMAGE_TAG" && -z "$HBUTOJ_MYSQL_CHECKER_IMAGE_TAG" ]]; then
  echo "ERROR: No image tag provided. Set HBUTOJ_IMAGE_TAG or per-component tags (HBUTOJ_BACKEND_IMAGE_TAG/HBUTOJ_FRONTEND_IMAGE_TAG/HBUTOJ_JUDGESERVER_IMAGE_TAG/HBUTOJ_MYSQL_CHECKER_IMAGE_TAG)." >&2
  exit 1
fi

HBUTOJ_BACKEND_IMAGE_TAG="${HBUTOJ_BACKEND_IMAGE_TAG:-$HBUTOJ_IMAGE_TAG}"
HBUTOJ_FRONTEND_IMAGE_TAG="${HBUTOJ_FRONTEND_IMAGE_TAG:-$HBUTOJ_IMAGE_TAG}"
HBUTOJ_JUDGESERVER_IMAGE_TAG="${HBUTOJ_JUDGESERVER_IMAGE_TAG:-$HBUTOJ_IMAGE_TAG}"
HBUTOJ_MYSQL_IMAGE_TAG="${HBUTOJ_MYSQL_IMAGE_TAG:-$HBUTOJ_IMAGE_TAG}"
HBUTOJ_MYSQL_CHECKER_IMAGE_TAG="${HBUTOJ_MYSQL_CHECKER_IMAGE_TAG:-$HBUTOJ_IMAGE_TAG}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: missing command: $1" >&2
    exit 1
  }
}

require_cmd docker
require_cmd git
require_cmd mvn
require_cmd npm
require_cmd node

MVN_QUIET_FLAG=""
if [[ "$HBUTOJ_MVN_QUIET" == "true" ]]; then
  MVN_QUIET_FLAG="-q"
fi

if [[ ! -d "$DEPLOY_REPO/src/backend" || ! -d "$DEPLOY_REPO/src/frontend" || ! -d "$DEPLOY_REPO/src/judgeserver" ]]; then
  echo "ERROR: DEPLOY_REPO does not look like hbutoj_deplay: $DEPLOY_REPO" >&2
  exit 1
fi

BUILD_ID="$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)"

echo "==> Build id: $BUILD_ID"

# 1) Build backend + judgeserver jars
pushd "$ROOT_DIR/hbutoj-springboot" >/dev/null

echo "==> Building backend jar (DataBackup)"
mvn $MVN_QUIET_FLAG -pl DataBackup -am clean package -DskipTests
BACKEND_JAR="$(ls -1 DataBackup/target/hoj-backend-*.jar | head -n 1)"

echo "==> Building judgeserver jar (JudgeServer)"
mvn $MVN_QUIET_FLAG -pl JudgeServer -am clean package -DskipTests
JUDGESERVER_JAR="$(ls -1 JudgeServer/target/hoj-judgeServer-*.jar | head -n 1)"

popd >/dev/null

# 2) Build frontend assets
pushd "$ROOT_DIR/hbutoj-vue" >/dev/null

echo "==> Building frontend assets (npm ci + npm run build)"
echo "==> Using npm registry: $HBUTOJ_NPM_REGISTRY"
if [[ -f package-lock.json ]]; then
  npm ci --no-audit --no-fund --registry "$HBUTOJ_NPM_REGISTRY"
else
  npm install --no-audit --no-fund --registry "$HBUTOJ_NPM_REGISTRY"
fi

NODE_MAJOR="$(node -p "process.versions.node.split('.')[0]" 2>/dev/null || echo 0)"
if [[ "$NODE_MAJOR" -ge 17 ]]; then
  export NODE_OPTIONS="${NODE_OPTIONS:-} --openssl-legacy-provider"
fi

npm run build

FRONTEND_DIST="$ROOT_DIR/hbutoj-vue/dist"
if [[ ! -d "$FRONTEND_DIST" ]]; then
  echo "ERROR: frontend build did not produce dist/: $FRONTEND_DIST" >&2
  exit 1
fi

popd >/dev/null

# 3) Copy artifacts into deploy repo build contexts

echo "==> Copy backend jar into deploy repo"
rm -f "$DEPLOY_REPO/src/backend"/*.jar
cp -f "$ROOT_DIR/hbutoj-springboot/$BACKEND_JAR" "$DEPLOY_REPO/src/backend/hoj-backend.jar"

echo "==> Copy judgeserver jar into deploy repo"
rm -f "$DEPLOY_REPO/src/judgeserver"/*.jar
cp -f "$ROOT_DIR/hbutoj-springboot/$JUDGESERVER_JAR" "$DEPLOY_REPO/src/judgeserver/"

echo "==> Copy frontend dist into deploy repo"
rm -rf "$DEPLOY_REPO/src/frontend/html"/*
cp -a "$FRONTEND_DIST"/. "$DEPLOY_REPO/src/frontend/html/"

echo "==> Sync scrollBoard into deploy repo"
rm -rf "$DEPLOY_REPO/src/frontend/scrollBoard"/*
cp -a "$ROOT_DIR/hbutoj-scrollBoard"/. "$DEPLOY_REPO/src/frontend/scrollBoard/"

# 4) Build + push images

docker_build_push() {
  local context_dir="$1"
  local image_prefix="$2"
  local image_repo="$3"
  local image_tag="$4"
  local full_image="$image_prefix/$image_repo:$image_tag"

  echo "==> docker build: $full_image (context: $context_dir)"
  docker build -t "$full_image" "$context_dir"

  if [[ "$HBUTOJ_PUSH" == "true" ]]; then
    echo "==> docker push: $full_image"
    docker push "$full_image"
  else
    echo "==> skip push (HBUTOJ_PUSH=$HBUTOJ_PUSH): $full_image"
  fi
}

docker_build_push "$DEPLOY_REPO/src/backend" "$HBUTOJ_IMAGE_PREFIX" "$HBUTOJ_BACKEND_IMAGE" "$HBUTOJ_BACKEND_IMAGE_TAG"
docker_build_push "$DEPLOY_REPO/src/frontend" "$HBUTOJ_IMAGE_PREFIX" "$HBUTOJ_FRONTEND_IMAGE" "$HBUTOJ_FRONTEND_IMAGE_TAG"
docker_build_push "$DEPLOY_REPO/src/judgeserver" "$HBUTOJ_IMAGE_PREFIX" "$HBUTOJ_JUDGESERVER_IMAGE" "$HBUTOJ_JUDGESERVER_IMAGE_TAG"

if [[ "$HBUTOJ_BUILD_MYSQL_CHECKER" == "true" ]]; then
  if [[ -z "$HBUTOJ_MYSQL_CHECKER_IMAGE_PREFIX" ]]; then
    echo "ERROR: HBUTOJ_MYSQL_CHECKER_IMAGE_PREFIX is empty. Set it explicitly or set HBUTOJ_IMAGE_PREFIX." >&2
    exit 1
  fi
  docker_build_push "$DEPLOY_REPO/src/mysql-checker" "$HBUTOJ_MYSQL_CHECKER_IMAGE_PREFIX" "$HBUTOJ_MYSQL_CHECKER_IMAGE" "$HBUTOJ_MYSQL_CHECKER_IMAGE_TAG"
else
  echo "==> skip mysql-checker image (HBUTOJ_BUILD_MYSQL_CHECKER=$HBUTOJ_BUILD_MYSQL_CHECKER)"
fi

if [[ "$HBUTOJ_BUILD_MYSQL" == "true" ]]; then
  if [[ -z "$HBUTOJ_MYSQL_IMAGE_PREFIX" ]]; then
    echo "ERROR: HBUTOJ_MYSQL_IMAGE_PREFIX is empty. Set it explicitly or set HBUTOJ_IMAGE_PREFIX." >&2
    exit 1
  fi
  docker_build_push "$DEPLOY_REPO/src/mysql" "$HBUTOJ_MYSQL_IMAGE_PREFIX" "$HBUTOJ_MYSQL_IMAGE" "$HBUTOJ_MYSQL_IMAGE_TAG"
else
  echo "==> skip mysql image (HBUTOJ_BUILD_MYSQL=$HBUTOJ_BUILD_MYSQL)"
fi

echo "==> Done. Next on server:"
echo "    cd $DEPLOY_REPO/standAlone && docker compose pull && docker compose up -d"
