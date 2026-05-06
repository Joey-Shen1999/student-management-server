# Test Deployment Runbook

The `main` branch deploys to the test server through `.github/workflows/deploy.yml`.

Expected GitHub Actions variables:

- `AWS_HOST=3.149.1.120`
- `AWS_USER=ubuntu`
- `AWS_PORT=22`
- `DEPLOY_PATH=/home/ubuntu/student-management-server`
- `SERVICE_NAME=student-management-server`

Expected GitHub Actions secrets:

- `AWS_SSH_KEY`
- `DB_PASSWORD`

The backend runs with the `prod` Spring profile on the test server. Because `application-prod.properties` reads datasource settings from environment variables, systemd must load:

```text
/etc/student-management/env/test.env
```

Use `ops/test/test.env.example` as the template. Do not commit the real password.

Required service line:

```ini
EnvironmentFile=/etc/student-management/env/test.env
```

Verification:

```bash
sudo systemctl status student-management-server --no-pager
ss -ltnp | grep 8080
curl -i http://127.0.0.1:8080/api/
curl -i http://127.0.0.1/api/
```

If Nginx returns `502 Bad Gateway`, check:

```bash
sudo journalctl -u student-management-server -n 120 --no-pager
sudo tail -n 80 /var/log/nginx/error.log
```
