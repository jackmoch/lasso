# Deployment Secrets Configuration

This document describes the required GitHub secrets and Google Cloud Secret Manager secrets for deploying Lasso to Google Cloud Run.

## GitHub Secrets

Configure these secrets in your GitHub repository settings (Settings → Secrets and variables → Actions):

### Required for Deployment (Sprint 8)

| Secret Name | Description | How to Obtain |
|-------------|-------------|---------------|
| `GCP_SA_KEY` | Google Cloud Service Account JSON key | Create service account in GCP with Cloud Run Admin + Secret Manager Secret Accessor roles, download JSON key |
| `GCP_PROJECT_ID` | Google Cloud Project ID | Found in GCP Console project selector |

## Google Cloud Secret Manager

Store these secrets in Google Cloud Secret Manager (Security → Secret Manager):

### Application Secrets

| Secret Name | Description | How to Generate |
|-------------|-------------|-----------------|
| `lastfm-api-key` | Last.fm API key | Register application at https://www.last.fm/api/account/create |
| `lastfm-api-secret` | Last.fm API shared secret | Provided with API key registration |
| `session-secret` | Session encryption key | Generate with `openssl rand -base64 32` |

## Setup Instructions

### 1. Create Google Cloud Service Account

```bash
# Set your project ID
PROJECT_ID="your-project-id"

# Create service account
gcloud iam service-accounts create lasso-deploy \
  --display-name="Lasso Deployment Account" \
  --project=$PROJECT_ID

# Grant necessary roles
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:lasso-deploy@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:lasso-deploy@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:lasso-deploy@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

# Create and download key
gcloud iam service-accounts keys create lasso-deploy-key.json \
  --iam-account=lasso-deploy@${PROJECT_ID}.iam.gserviceaccount.com

# Copy contents of lasso-deploy-key.json to GCP_SA_KEY GitHub secret
cat lasso-deploy-key.json
```

### 2. Store Application Secrets in Secret Manager

```bash
# Last.fm API credentials
echo -n "your-lastfm-api-key" | gcloud secrets create lastfm-api-key --data-file=-
echo -n "your-lastfm-api-secret" | gcloud secrets create lastfm-api-secret --data-file=-

# Generate and store session secret
openssl rand -base64 32 | gcloud secrets create session-secret --data-file=-

# Verify secrets created
gcloud secrets list
```

### 3. Configure GitHub Secrets

1. Go to your GitHub repository
2. Navigate to Settings → Secrets and variables → Actions
3. Click "New repository secret"
4. Add each secret:
   - Name: `GCP_SA_KEY`, Value: [contents of lasso-deploy-key.json]
   - Name: `GCP_PROJECT_ID`, Value: [your project ID]

## Security Best Practices

- **Never commit secrets to version control** - All secrets should be stored in Secret Manager or GitHub Secrets
- **Rotate secrets regularly** - Update API keys and session secrets periodically
- **Use least privilege** - Service account only has necessary permissions
- **Delete local key files** - After uploading to GitHub, securely delete `lasso-deploy-key.json`
- **Enable audit logging** - Monitor Secret Manager access in Cloud Logging

## Verification

To verify your deployment secrets are configured correctly:

```bash
# Check service account permissions
gcloud projects get-iam-policy $PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:lasso-deploy@${PROJECT_ID}.iam.gserviceaccount.com"

# Check Secret Manager secrets exist
gcloud secrets list --filter="name:lastfm OR name:session"

# Test accessing a secret (from service account context)
gcloud secrets versions access latest --secret=lastfm-api-key
```

## Troubleshooting

**Error: Permission denied accessing secrets**
- Ensure service account has `secretmanager.secretAccessor` role
- Verify Cloud Run service account can access secrets

**Error: Invalid service account key**
- Regenerate JSON key and update GitHub secret
- Ensure key hasn't been deleted in GCP Console

**Error: Cloud Run deployment fails**
- Check service account has `run.admin` and `iam.serviceAccountUser` roles
- Verify project ID is correct in GitHub secret

## TODO for Sprint 8

- [ ] Set up Google Cloud Project
- [ ] Register Last.fm API application
- [ ] Create service account and configure IAM
- [ ] Store secrets in Secret Manager
- [ ] Add GitHub repository secrets
- [ ] Test deployment workflow
- [ ] Document staging vs production environment configuration
- [ ] Set up custom domain (optional)
