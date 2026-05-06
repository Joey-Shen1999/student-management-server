# Deployment Guide (GitHub Actions -> AWS Ubuntu)

DB safety runbook:
- `docs/db-preflight-and-rollback.md`

## 1. GitHub Actions Configuration
Configure in `Repository -> Settings -> Secrets and variables -> Actions`.

### Secrets
- `AWS_SSH_KEY`: private key used by GitHub Actions to SSH to the server (`ubuntu` user).
- `SPRING_MAIL_PASSWORD`: Google Workspace/Gmail app password for SMTP sending.

### Variables
- `AWS_HOST`: `3.23.21.144`
- `AWS_USER`: `ubuntu`
- `AWS_PORT`: `22`
- `DEPLOY_PATH`: `/home/ubuntu/student-management-server`
- `SERVICE_NAME`: `student-management-server`
- `DB_CONTAINER_NAME` (optional): `uni_apply_db`
- `DB_HOST` (optional): `localhost`
- `DB_PORT` (optional): `5432`
- `DB_NAME` (optional): `uni_apply`
- `DB_USER` (optional): `postgres`
- `SPRING_MAIL_USERNAME` (optional): defaults to `noreply@global-vip.ca`
- `APP_MAIL_FROM` (optional): defaults to `noreply@global-vip.ca`
- `APP_INFO_TASK_EMAIL_REMINDERS_ENABLED` (optional): defaults to `true`
- `APP_TASK_TRACKING_EMAIL_REMINDERS_ENABLED` (optional): defaults to `true`

### Optional DB Secret
- `DB_PASSWORD` (optional): PostgreSQL password used by migration command.
  - If omitted, workflow falls back to `postgres`.

## 2. Critical Prerequisite (Server must be able to fetch GitHub code)
The workflow SSHes into the server and runs:
- `git fetch --all --prune`
- `git reset --hard origin/master`

So the server itself must have repository access.

Check on server:

```bash
whoami
cd /home/ubuntu/student-management-server
git remote -v
```

If the repo is private, prefer SSH remote + Deploy Key.

### 2.1 Configure server SSH key for GitHub (recommended)
Generate key on server:

```bash
ssh-keygen -t ed25519 -C "deploy@student-management-server" -f ~/.ssh/id_ed25519_github_deploy -N ""
```

Print public key:

```bash
cat ~/.ssh/id_ed25519_github_deploy.pub
```

Add this public key to GitHub repo:
- `Repository -> Settings -> Deploy keys -> Add deploy key`
- enable `Allow write access` only if you really need write operations (not needed for this workflow).

Create SSH config on server:

```bash
cat > ~/.ssh/config <<'EOF'
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519_github_deploy
  IdentitiesOnly yes
EOF
chmod 600 ~/.ssh/config
```

Switch repo remote to SSH:

```bash
cd /home/ubuntu/student-management-server
git remote set-url origin git@github.com:Joey-Shen1999/student-management-server.git
git remote -v
```

Verify:

```bash
ssh -T git@github.com
```

## 3. Server Initialization
Install required packages:

```bash
sudo apt update
sudo apt install -y git curl ca-certificates openjdk-17-jdk
java -version
```

If your project does not use `mvnw`, install Maven:

```bash
sudo apt install -y maven
mvn -v
```

Prepare deployment directory:

```bash
sudo mkdir -p /home/ubuntu/student-management-server
sudo chown -R ubuntu:ubuntu /home/ubuntu/student-management-server
```

Clone repository (first time only):

```bash
git clone git@github.com:Joey-Shen1999/student-management-server.git /home/ubuntu/student-management-server
cd /home/ubuntu/student-management-server
git checkout main
```

## 4. systemd Service Setup
Copy service file:

```bash
sudo cp /home/ubuntu/student-management-server/student-management-server.service /etc/systemd/system/student-management-server.service
sudo systemctl daemon-reload
sudo systemctl enable student-management-server
```

Do initial build:

```bash
cd /home/ubuntu/student-management-server
if [ -f mvnw ]; then
  chmod +x mvnw
  ./mvnw clean package -DskipTests
else
  mvn clean package -DskipTests
fi
```

Create stable jar symlink used by systemd:

```bash
cd /home/ubuntu/student-management-server
JAR_PATH="$(find target -maxdepth 1 -type f -name '*.jar' \
  ! -name 'original-*.jar' \
  ! -name '*-sources.jar' \
  ! -name '*-javadoc.jar' \
  -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)"
ln -sfn "$(basename "$JAR_PATH")" target/student-management-server-latest.jar
ls -l target/student-management-server-latest.jar
```

Start service:

```bash
sudo systemctl start student-management-server
sudo systemctl status student-management-server --no-pager
```

## 5. Passwordless sudo (limited to this service)
Edit sudoers:

```bash
sudo visudo -f /etc/sudoers.d/student-management-server
```

Add:

```text
ubuntu ALL=(root) NOPASSWD: /bin/systemctl restart student-management-server, /bin/systemctl status student-management-server, /usr/bin/systemctl restart student-management-server, /usr/bin/systemctl status student-management-server
```

Set proper permissions:

```bash
sudo chmod 440 /etc/sudoers.d/student-management-server
```

## 6. Workflow Behavior
Production changes should flow through `feature/*` -> PR to `main` -> test on `main` -> manual merge from `main` to `master`.

On each push to `master`, `.github/workflows/deploy-production.yml` builds and deploys the production jar to EC2. The legacy `.github/workflows/deploy.yml` is manual-only and should not be used for normal production releases.

The legacy manual workflow does:
1. Checkout workflow repo
2. Validate vars
3. Configure SSH private key + known_hosts
4. SSH to server and run:
   - `cd $DEPLOY_PATH`
   - `git fetch --all --prune`
   - `git reset --hard origin/master`
   - `git clean -fd`
   - writes `config/local-secrets.properties` from GitHub Actions mail secrets/variables when `SPRING_MAIL_PASSWORD` is configured
   - run DB preflight (read-only):
     - `scripts/ops/preflight_release_20260330.sql`
     - deploy is blocked if preflight fails
   - run migrations:
     - `scripts/migrations/20260318_teacher_student_ownership_deprecation.sql`
     - `scripts/migrations/20260323_add_student_profile_teacher_note.sql`
     - `scripts/migrations/20260327_add_performance_indexes.sql`
     - prefer `docker exec $DB_CONTAINER_NAME ... psql`
     - fallback to local `psql` client
   - build (`./mvnw` or `mvn`)
   - pick latest runnable jar
   - update symlink `target/student-management-server-latest.jar`
   - `sudo systemctl restart $SERVICE_NAME`
   - `sudo systemctl status $SERVICE_NAME --no-pager`

## 7. Troubleshooting

### 7.1 Actions fails before SSH
- Ensure all required Variables and Secret are configured.
- Ensure `AWS_SSH_KEY` is the correct private key for server login.

### 7.2 SSH login fails
- Check AWS security group and firewall allow SSH port.
- Validate username (`ubuntu`) and port.
- Confirm key in `/home/ubuntu/.ssh/authorized_keys`.

### 7.3 SSH succeeds but `git fetch` fails
Typical errors:
- `Permission denied (publickey)`
- `fatal: could not read Username for 'https://github.com'`

Fix:
- Ensure server repo remote is SSH (`git@github.com:...`)
- Ensure Deploy Key is installed and valid.

### 7.4 Build fails
- Check Java version and Maven/Maven Wrapper.
- Check server disk/memory.
- Run build manually on server to reproduce.

### 7.4.1 DB preflight fails
- Read failure details from deployment logs.
- Run the same SQL manually:
  - `scripts/ops/preflight_release_20260330.sql`
- Resolve blocking DB issues first, then re-run deployment.
- Use rollback guidance in `docs/db-preflight-and-rollback.md`.

### 7.5 Service restart/startup fails
Check:

```bash
sudo systemctl status student-management-server --no-pager
sudo journalctl -u student-management-server -n 200 --no-pager
ls -l /home/ubuntu/student-management-server/target/student-management-server-latest.jar
```

Common causes:
- missing jar/symlink
- DB/env config errors
- wrong Java version

### 7.6 `sudo` prompts password in workflow
- Recheck `/etc/sudoers.d/student-management-server`
- Ensure both `/bin/systemctl` and `/usr/bin/systemctl` are allowed.
