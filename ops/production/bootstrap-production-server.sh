#!/usr/bin/env bash
set -euo pipefail

# Production bootstrap script for Ubuntu 24.04 ARM64.
# Run on EC2 host as ubuntu user: bash ops/production/bootstrap-production-server.sh

if [[ "${EUID}" -eq 0 ]]; then
  echo "Please run as ubuntu user (not root)."
  exit 1
fi

sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get -y upgrade
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
  openjdk-21-jdk \
  postgresql \
  postgresql-contrib \
  nginx \
  git \
  unzip \
  curl \
  rsync

sudo mkdir -p /home/ubuntu/student-management-server
sudo chown -R ubuntu:ubuntu /home/ubuntu/student-management-server

sudo mkdir -p /var/www/student-management-frontend
sudo chown -R www-data:www-data /var/www/student-management-frontend

sudo mkdir -p /etc/student-management
sudo mkdir -p /etc/student-management/env
sudo chown -R root:root /etc/student-management
sudo chmod 750 /etc/student-management
sudo chmod 750 /etc/student-management/env

if [[ ! -f /etc/student-management/env/production.env ]]; then
  sudo install -m 640 -o root -g ubuntu ops/production/production.env.example /etc/student-management/env/production.env
  echo "Created /etc/student-management/env/production.env (update secrets before service start)."
fi

if [[ ! -f /home/ubuntu/student-management-server/application-prod.properties ]]; then
  install -m 640 -o ubuntu -g ubuntu ops/production/application-prod.properties.example /home/ubuntu/student-management-server/application-prod.properties
fi

sudo cp ops/production/student-management-server.service /etc/systemd/system/student-management-server.service
sudo cp ops/production/student-management-platform.nginx.conf /etc/nginx/sites-available/student-management-platform
sudo ln -sfn /etc/nginx/sites-available/student-management-platform /etc/nginx/sites-enabled/student-management-platform
if [[ -L /etc/nginx/sites-enabled/default ]]; then
  sudo rm -f /etc/nginx/sites-enabled/default
fi

sudo nginx -t
sudo systemctl reload nginx

sudo systemctl daemon-reload
sudo systemctl enable student-management-server
sudo systemctl enable nginx
sudo systemctl enable postgresql

echo "Bootstrap complete."
echo "Next steps:"
echo "1) Edit /etc/student-management/env/production.env"
echo "2) Ensure /home/ubuntu/student-management-server/student-management-server-latest.jar exists"
echo "3) sudo systemctl restart student-management-server"
