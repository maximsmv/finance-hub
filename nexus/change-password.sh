ADMIN_PASS_FILE=${NEXUS_ADMIN_PASSWORD_FILE}
NEW_PASS=${NEXUS_NEW_PASSWORD}

if [ -z "$NEW_PASS" ]; then
  echo "$NEW_PASS"
  echo "New password is empty"
  exit 1
fi

if [ ! -s "$ADMIN_PASS_FILE" ]; then
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] Admin password wass successfully changed" >&2
  exit 0
fi

ADMIN_PASS=$(cat "$ADMIN_PASS_FILE")

echo "Changing admin password..."
curl -sS -u "admin:${ADMIN_PASS}" \
  -X PUT "http://nexus:8081/service/rest/v1/security/users/admin/change-password" \
  -H "Content-Type: text/plain" \
  --data "${NEW_PASS}" \
  && echo "Password changed successfully." \
  || { echo "Failed to change password"; exit 1; }

echo "Disabling anonymous access in Nexus..."
curl -s -o /dev/null -w "%{http_code}" -u "admin:${NEW_PASS}" \
  -H "Content-Type: application/json" \
  -X PUT http://nexus:8081/service/rest/v1/security/anonymous \
  -d '{"enabled": true}'
