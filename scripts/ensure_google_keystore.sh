#!/usr/bin/env bash
# Create a key only for a new project. Existing/invalid keys are never replaced.
set -euo pipefail
umask 077

cd "$(dirname "$0")/.."
# Read the same channel configuration as Gradle. Values are data, never shell code.
# Passwords requiring Java-properties escaping can be supplied through CI environment variables.
config_file=app/src/google/sign.properties
config_value() {
    local value
    value=$(sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*//p" "$config_file" | tr -d '\r')
    if [[ -z "$value" ]]; then
        echo "Missing $1 in $config_file" >&2
        return 1
    fi
    if [[ "$value" == *\\* || "$value" == *$'\n'* ]]; then
        echo "Use a single literal value for $1 in $config_file, or override it through the environment" >&2
        return 1
    fi
    printf '%s' "$value"
}
# Separate assignments from export so a missing/invalid setting fails under set -e.
ANDROID_SIGNING_STORE_FILE="${ANDROID_SIGNING_STORE_FILE:-app/src/google/$(config_value storeFile)}"
ANDROID_SIGNING_STORE_PASSWORD="${ANDROID_SIGNING_STORE_PASSWORD:-$(config_value storePassword)}"
ANDROID_SIGNING_KEY_ALIAS="${ANDROID_SIGNING_KEY_ALIAS:-$(config_value keyAlias)}"
ANDROID_SIGNING_KEY_PASSWORD="${ANDROID_SIGNING_KEY_PASSWORD:-$(config_value keyPassword)}"
export ANDROID_SIGNING_STORE_FILE ANDROID_SIGNING_STORE_PASSWORD ANDROID_SIGNING_KEY_ALIAS ANDROID_SIGNING_KEY_PASSWORD
created=false
mkdir -p "$(dirname "$ANDROID_SIGNING_STORE_FILE")"

if [[ ! -e "$ANDROID_SIGNING_STORE_FILE" ]]; then
    # Release identity is created by the lcb4 workflow, never by a developer machine.
    if [[ "${GITHUB_ACTIONS:-}" != true || "${GITHUB_REF_NAME:-}" != lcb4 ]]; then
        echo "Missing signing key. Run the GitHub Actions workflow on lcb4 to create and persist it." >&2
        exit 1
    fi
    # JKS supports distinct key/store passwords; keytool receives passwords through env.
    keytool -genkeypair -storetype JKS \
        -keystore "$ANDROID_SIGNING_STORE_FILE" \
        -storepass:env ANDROID_SIGNING_STORE_PASSWORD \
        -keypass:env ANDROID_SIGNING_KEY_PASSWORD \
        -alias "$ANDROID_SIGNING_KEY_ALIAS" \
        -keyalg RSA -keysize 2048 -validity 36500 \
        -dname "CN=Google Release, OU=Mobile, O=HealthTracker, L=Unknown, ST=Unknown, C=US"
    created=true
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "created=$created" >> "$GITHUB_OUTPUT"
fi

# Fail on a corrupt/empty keystore or incorrect credentials, rather than rotate the key.
# A CSR validates the private-key password without changing the certificate or keystore.
keytool -certreq -keystore "$ANDROID_SIGNING_STORE_FILE" \
    -storepass:env ANDROID_SIGNING_STORE_PASSWORD \
    -keypass:env ANDROID_SIGNING_KEY_PASSWORD \
    -alias "$ANDROID_SIGNING_KEY_ALIAS" > /dev/null
echo "Google signing key ready (created=$created): $ANDROID_SIGNING_STORE_FILE"
