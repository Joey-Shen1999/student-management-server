#!/usr/bin/env bash
set -euo pipefail

# Ensure backend only listens on localhost and public traffic goes through nginx.

sudo sed -i 's/^#\?listen_addresses.*/listen_addresses = '\''localhost'\''/' /etc/postgresql/*/main/postgresql.conf
sudo systemctl restart postgresql

if command -v ufw >/dev/null 2>&1; then
  sudo ufw allow OpenSSH
  sudo ufw allow 80/tcp
  sudo ufw deny 8080/tcp
  sudo ufw --force enable
fi

echo "Network hardening complete."
echo "Verify backend bind: ss -lntp | grep 8080"
