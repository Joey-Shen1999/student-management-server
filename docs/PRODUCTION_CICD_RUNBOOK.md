# Production CI/CD Runbook (EC2 Ubuntu 24.04 ARM64)

## 1) GitHub Secrets (Repository: student-management-server)
Set these in `Settings -> Secrets and variables -> Actions -> Secrets`:

- `PROD_HOST=3.23.21.144`
- `PROD_USER=ubuntu`
- `PROD_PORT=22`
- `PROD_DEPLOY_PATH=/home/ubuntu/student-management-server`
- `PROD_SERVICE_NAME=student-management-server`
- `PROD_SSH_KEY=<contents of gedu.joey.openssh>`

Public site URL:

- `https://globalvip-studentportal.ca`
- `https://www.globalvip-studentportal.ca`

Production SSH deployment should continue to use the Elastic IP through `PROD_HOST=3.23.21.144`.

Set this in `Settings -> Secrets and variables -> Actions -> Variables`:

- `PROD_FRONTEND_REPOSITORY=<owner>/student-management-frontend`

## 2) EC2 one-time bootstrap
Run on EC2:

```bash
cd /home/ubuntu
git clone <your-server-repo-url> student-management-server
cd student-management-server
bash ops/production/bootstrap-production-server.sh
```

## 3) Initialize PostgreSQL
Run on EC2 (replace password):

```bash
cd /home/ubuntu/student-management-server
APP_DB_NAME=student_management_prod \
APP_DB_USER=student_app \
APP_DB_PASSWORD='REPLACE_STRONG_PASSWORD' \
bash ops/production/init-postgresql.sh
```

## 4) Environment configuration
Edit:

- `/etc/student-management/env/production.env`
- `/home/ubuntu/student-management-server/application-prod.properties`

Then lock permissions:

```bash
sudo chown root:ubuntu /etc/student-management/env/production.env
sudo chmod 640 /etc/student-management/env/production.env
sudo chown ubuntu:ubuntu /home/ubuntu/student-management-server/application-prod.properties
sudo chmod 640 /home/ubuntu/student-management-server/application-prod.properties
```

## 5) Enforce localhost-only backend + firewall policy
Run:

```bash
cd /home/ubuntu/student-management-server
bash ops/production/harden-network.sh
```

Expected:

- backend bind: `127.0.0.1:8080`
- public ingress: `80` only (+ SSH `22`)

## 6) Service management

```bash
sudo systemctl daemon-reload
sudo systemctl enable student-management-server
sudo systemctl restart student-management-server
sudo systemctl status student-management-server --no-pager
```

Logs:

```bash
journalctl -u student-management-server -n 200 --no-pager
journalctl -u student-management-server -f
```

## 7) Nginx validation

```bash
sudo nginx -t
sudo systemctl reload nginx
sudo systemctl status nginx --no-pager
```

The production Nginx `server_name` should be:

```nginx
server_name globalvip-studentportal.ca www.globalvip-studentportal.ca;
```

Direct browser access by Elastic IP should stay closed. The HTTP default server may return `444` for `3.23.21.144` and `_`, while the domain server handles public traffic.

Keep `/api` proxied without stripping the `/api` prefix:

```nginx
proxy_pass http://127.0.0.1:8080;
```

## 7.1) HTTPS

After Porkbun DNS points both root and `www` A records to `3.23.21.144`, run on the production EC2:

```bash
sudo apt update
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d globalvip-studentportal.ca -d www.globalvip-studentportal.ca
sudo certbot renew --dry-run
```

Choose the Certbot option to redirect HTTP to HTTPS.

## 8) Verification checklist

Backend:

```bash
curl -i http://127.0.0.1:8080
ss -lntp | grep 8080
```

Frontend:

```bash
ls -la /var/www/student-management-frontend
curl -i http://127.0.0.1/
```

API proxy:

```bash
curl -i http://127.0.0.1/api/auth/login
```

PostgreSQL:

```bash
sudo -u postgres psql -c "\l"
sudo -u postgres psql -d student_management_prod -c "select now();"
```

CI/CD:

- Merge PR -> `master`
- Confirm GitHub Action `Production CI/CD` green
- Confirm EC2 updates:
  - `/home/ubuntu/student-management-server/student-management-server-latest.jar`
  - `/var/www/student-management-frontend/index.html`

## 9) Rollback
Rollback backend jar:

```bash
cd /home/ubuntu/student-management-server
cp student-management-server-latest.jar "rollback-$(date +%F-%H%M%S).jar"
# replace with previous known-good jar name:
cp <previous-good>.jar student-management-server-latest.jar
sudo systemctl restart student-management-server
```

Rollback frontend:

```bash
sudo rsync -a --delete /var/www/student-management-frontend-backup/<timestamp>/ /var/www/student-management-frontend/
sudo systemctl reload nginx
```

## 10) Branch protection (master)
Apply with GitHub CLI:

```bash
cd /home/ubuntu/student-management-server
export GITHUB_REPOSITORY=<owner>/student-management-server
gh auth login
bash ops/production/apply-branch-protection.sh
```

This enforces:

- PR required before merge
- at least one approval
- required status checks
- branch up-to-date before merge
- no force-push
- no branch deletion
- linear history
