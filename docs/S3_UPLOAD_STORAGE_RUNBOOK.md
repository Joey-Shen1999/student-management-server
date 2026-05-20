# S3 Upload Storage Runbook

This app supports two upload storage modes:

- `local`: files are stored under `/home/ubuntu/student-management-server/uploads`.
- `s3`: files are stored in a private S3 bucket through the EC2 instance IAM role.

Keep the S3 bucket private. Do not make uploaded student files public.

## 1. S3 Bucket

Recommended production bucket:

```text
globalvip-studentportal-prod-uploads-680458885427-us-east-2-an
```

Recommended bucket settings:

- Region: `us-east-2`
- Object ownership: ACLs disabled / bucket owner enforced
- Block all public access: enabled
- Versioning: enabled
- Encryption: SSE-S3

## 2. IAM Policy

Create an IAM policy and replace the bucket name if needed:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::globalvip-studentportal-prod-uploads-680458885427-us-east-2-an"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::globalvip-studentportal-prod-uploads-680458885427-us-east-2-an/student-management-prod/*"
    }
  ]
}
```

Attach this policy to an IAM role trusted by EC2, then attach the role to the production EC2 instance.

## 3. Production App Config

Set these in the production service environment file:

```bash
APP_STORAGE_TYPE=s3
APP_STORAGE_S3_REGION=us-east-2
APP_STORAGE_S3_BUCKET=globalvip-studentportal-prod-uploads-680458885427-us-east-2-an
APP_STORAGE_S3_PREFIX=student-management-prod
```

Then restart:

```bash
sudo systemctl daemon-reload
sudo systemctl restart student-management-server
sudo systemctl status student-management-server --no-pager
```

## 4. Existing Local File Migration

Run after the EC2 role can access the bucket:

```bash
aws s3 sync /home/ubuntu/student-management-server/uploads/student-documents/ \
  s3://globalvip-studentportal-prod-uploads-680458885427-us-east-2-an/student-management-prod/student-documents/

aws s3 sync /home/ubuntu/student-management-server/uploads/student-identity-files/ \
  s3://globalvip-studentportal-prod-uploads-680458885427-us-east-2-an/student-management-prod/student-identity-files/

aws s3 sync /home/ubuntu/student-management-server/uploads/student-school-transcripts/ \
  s3://globalvip-studentportal-prod-uploads-680458885427-us-east-2-an/student-management-prod/student-school-transcripts/
```

Do not delete local files until upload, download, and delete are verified in production.

## 5. Verification

Check EC2 can see the bucket:

```bash
aws s3 ls s3://globalvip-studentportal-prod-uploads-680458885427-us-east-2-an/student-management-prod/
```

Then verify through the app:

- Upload a student document.
- Download the document from the app.
- Delete the document from the app.
- Confirm the object exists or is removed in S3.

```bash
aws s3 ls s3://globalvip-studentportal-prod-uploads-680458885427-us-east-2-an/student-management-prod/student-documents/
```
