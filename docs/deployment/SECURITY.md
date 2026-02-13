# Security Best Practices

This document outlines security measures implemented in Lasso and best practices for deployment.

## Table of Contents

1. [Security Overview](#security-overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Session Security](#session-security)
4. [API Security](#api-security)
5. [Data Protection](#data-protection)
6. [Infrastructure Security](#infrastructure-security)
7. [Security Headers](#security-headers)
8. [Rate Limiting](#rate-limiting)
9. [Vulnerability Management](#vulnerability-management)
10. [Incident Response](#incident-response)

## Security Overview

### Threat Model

**What we protect:**
- User Last.fm credentials (via OAuth, never stored)
- Session data and authentication tokens
- User listening history during active sessions
- API keys and secrets

**Attack vectors we mitigate:**
- ✅ Cross-Site Scripting (XSS)
- ✅ Cross-Site Request Forgery (CSRF)
- ✅ Session hijacking
- ✅ Man-in-the-middle attacks
- ✅ Rate limiting / DoS
- ✅ Injection attacks
- ✅ Information disclosure

### Security Principles

1. **Defense in Depth**: Multiple layers of security
2. **Least Privilege**: Minimal necessary permissions
3. **Secure by Default**: Secure configuration out of the box
4. **Zero Trust**: Verify everything, trust nothing
5. **Fail Secure**: Errors don't compromise security

## Authentication & Authorization

### OAuth 2.0 Flow

**Security measures:**
- ✅ Use OAuth instead of passwords (never handle user passwords)
- ✅ HTTPS required for all OAuth redirects
- ✅ State parameter for CSRF protection (Last.fm handles this)
- ✅ Short-lived authorization tokens (60 seconds)
- ✅ Server-side token exchange (client never sees tokens)

**Implementation:**
```clojure
;; OAuth initialization
(defn generate-auth-url []
  ;; Redirects to Last.fm over HTTPS
  ;; Last.fm generates and validates token
  (str auth-url "/api/auth/?api_key=" api-key
       "&cb=" (URLEncode callback-url)))

;; Token exchange (server-side only)
(defn get-session-key [token]
  ;; Exchange short-lived token for session key
  ;; Never expose session key to client
  (api-request {:method "auth.getSession"
                :params {:token token}
                :signed true}))
```

### Session Management

**Security measures:**
- ✅ HTTP-only cookies (not accessible via JavaScript)
- ✅ Secure cookies (HTTPS only in production)
- ✅ SameSite=Strict (CSRF protection)
- ✅ Server-side session storage (not client-side)
- ✅ Session invalidation on logout
- ✅ No session fixation (new session on login)

**Configuration:**
```bash
# .env.production
SESSION_COOKIE_SECURE=true         # HTTPS only
SESSION_COOKIE_HTTP_ONLY=true      # No JS access
SESSION_COOKIE_SAME_SITE=strict    # CSRF protection
SESSION_SECRET=<strong-random>      # Encryption key
```

## Session Security

### Encryption

**Session data encryption:**
```clojure
;; All session keys are encrypted before storage
(defn encrypt-session-key [session-key]
  (crypto/encrypt session-key (get-session-secret)))

;; Decrypted only when needed
(defn decrypt-session-key [encrypted-key]
  (crypto/decrypt encrypted-key (get-session-secret)))
```

### Session Secret

**Requirements:**
- ✅ Minimum 32 characters
- ✅ Random, unpredictable
- ✅ Unique per environment
- ✅ Rotated regularly (every 90 days)
- ✅ Stored in Secret Manager, not code

**Generate strong secret:**
```bash
# Use this to generate SESSION_SECRET
openssl rand -base64 32
```

### Session Lifetime

- Default: 24 hours
- Auto-refresh on activity
- Cleared on logout
- Invalidated on security events

## API Security

### Last.fm API Protection

**Rate limiting:**
```clojure
;; Respect Last.fm's 5 req/sec limit
(def min-interval-ms 200)

;; Client-side rate limiting
(defn wait-for-rate-limit []
  (let [elapsed (- now @last-request-time)]
    (when (< elapsed min-interval-ms)
      (Thread/sleep (- min-interval-ms elapsed)))))
```

**API signature validation:**
```clojure
;; All write operations require signature
(defn generate-api-signature [params]
  ;; MD5(sorted_params + api_secret)
  (crypto/md5 (str params api-secret)))
```

### Request Validation

**Input validation:**
- ✅ Username format validation
- ✅ Maximum request size limits
- ✅ Content-Type verification
- ✅ Parameter sanitization
- ✅ Malli schemas for validation

## Data Protection

### Data at Rest

**What we store:**
- Session metadata (username, state, timestamps)
- Encrypted Last.fm session keys
- Temporary scrobble cache (session duration only)

**What we DON'T store:**
- Passwords (OAuth only)
- Long-term listening history
- Personal information beyond username

**Storage security:**
- ✅ In-memory storage (cleared on restart)
- ✅ Encrypted session keys
- ✅ No persistent database (MVP)
- ✅ Session cleanup on logout/timeout

### Data in Transit

**TLS/HTTPS:**
- ✅ HTTPS enforced in production
- ✅ TLS 1.2+ required
- ✅ HSTS header (force HTTPS)
- ✅ Secure cookies only over HTTPS

**Cloud Run configuration:**
```bash
# HTTPS automatically enforced by Cloud Run
# HTTP requests redirected to HTTPS
# No additional configuration needed
```

### Data Minimization

**Principles:**
- Collect only what's needed for functionality
- Retain only for session duration
- Clear data on logout/stop
- No analytics or tracking

## Infrastructure Security

### Google Cloud Run

**Security features:**
- ✅ Container isolation
- ✅ Automatic TLS/HTTPS
- ✅ IAM-based access control
- ✅ VPC networking (optional)
- ✅ Secret Manager integration
- ✅ DDoS protection
- ✅ Audit logging

**Configuration:**
```bash
# Minimum required permissions
gcloud run services update lasso \
  --service-account=lasso-sa@project.iam.gserviceaccount.com \
  --no-allow-unauthenticated  # For private services
```

### Secrets Management

**Use Google Secret Manager:**
```bash
# Store sensitive values
gcloud secrets create lastfm-api-secret --data-file=- <<< "secret-value"
gcloud secrets create session-secret --data-file=- <<< "secret-value"

# Grant Cloud Run access
gcloud secrets add-iam-policy-binding lastfm-api-secret \
  --member="serviceAccount:SA_EMAIL" \
  --role="roles/secretmanager.secretAccessor"

# Reference in Cloud Run
gcloud run services update lasso \
  --set-secrets=LASTFM_API_SECRET=lastfm-api-secret:latest
```

**DON'T:**
- ❌ Hardcode secrets in code
- ❌ Store secrets in environment variables (visible in logs)
- ❌ Commit `.env.production` to Git
- ❌ Share secrets via insecure channels

### Network Security

**Cloud Run network configuration:**
```yaml
# Ingress control
ingress: all  # or internal-and-cloud-load-balancing

# VPC connector (for private resources)
vpc-access-connector: projects/PROJECT/locations/REGION/connectors/CONNECTOR

# Egress control
vpc-access-egress: private-ranges-only
```

## Security Headers

### Implemented Headers

**Strict-Transport-Security (HSTS):**
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```
- Forces HTTPS for 1 year
- Applies to all subdomains
- Preload list eligible

**Content-Security-Policy (CSP):**
```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'
```
- Prevents XSS attacks
- Restricts resource loading
- Allows inline styles (Tailwind)

**X-Content-Type-Options:**
```
X-Content-Type-Options: nosniff
```
- Prevents MIME sniffing
- Forces declared content types

**X-Frame-Options:**
```
X-Frame-Options: DENY
```
- Prevents clickjacking
- Blocks iframe embedding

**X-XSS-Protection:**
```
X-XSS-Protection: 1; mode=block
```
- Legacy XSS protection
- Blocks detected attacks

**Referrer-Policy:**
```
Referrer-Policy: strict-origin-when-cross-origin
```
- Limits referrer information
- Privacy protection

### Testing Headers

```bash
# Check security headers
curl -I https://your-app.run.app

# Use online tools
https://securityheaders.com
https://observatory.mozilla.org
```

## Rate Limiting

### Application-Level Rate Limiting

**Configuration:**
```bash
RATE_LIMIT_ENABLED=true
RATE_LIMIT_MAX_REQUESTS=100    # Per window
RATE_LIMIT_WINDOW_MS=60000     # 1 minute
```

**Implementation:**
```clojure
(def rate-limit-interceptor
  ;; Tracks requests per IP per minute
  ;; Returns 429 when limit exceeded
  ;; Includes Retry-After header
  )
```

**Response on limit exceeded:**
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
Content-Type: application/json

{"error": "Too many requests. Please try again later."}
```

### Cloud Run Concurrency

```bash
# Limit concurrent requests per instance
gcloud run services update lasso \
  --concurrency 80  # Max concurrent requests

# Autoscaling
gcloud run services update lasso \
  --min-instances 0 \
  --max-instances 10
```

## Vulnerability Management

### Dependency Scanning

**Automated scanning:**
```bash
# Check for vulnerable dependencies
clj -M:outdated
npm audit

# Fix vulnerabilities
npm audit fix

# Update dependencies regularly
```

### Security Updates

**Update schedule:**
- Critical vulnerabilities: Immediate
- High severity: Within 7 days
- Medium severity: Within 30 days
- Low severity: Next release cycle

**Monitoring:**
- GitHub Dependabot alerts
- npm audit in CI
- clj-watson for Clojure deps

### Penetration Testing

**Before production launch:**
- [ ] OWASP ZAP scan
- [ ] Manual security review
- [ ] OAuth flow testing
- [ ] Session security testing
- [ ] Rate limiting verification

## Incident Response

### Security Incident Procedure

1. **Detection**: Monitoring alerts or user report
2. **Assessment**: Determine severity and scope
3. **Containment**: Stop the attack
4. **Eradication**: Remove vulnerability
5. **Recovery**: Restore normal operations
6. **Lessons Learned**: Document and improve

### Emergency Actions

**If session secret compromised:**
```bash
# 1. Generate new secret immediately
openssl rand -base64 32

# 2. Update Secret Manager
gcloud secrets versions add session-secret --data-file=- <<< "new-secret"

# 3. Redeploy service (invalidates all sessions)
gcloud run services update lasso \
  --set-secrets=SESSION_SECRET=session-secret:latest

# 4. Monitor for suspicious activity
```

**If API keys leaked:**
```bash
# 1. Revoke keys at Last.fm immediately
# Go to: https://www.last.fm/api/account

# 2. Generate new API keys

# 3. Update secrets
gcloud secrets versions add lastfm-api-secret \
  --data-file=- <<< "new-secret"

# 4. Redeploy
```

### Logging for Security

**What to log:**
- ✅ Authentication attempts (success/failure)
- ✅ Session creation/destruction
- ✅ Rate limit violations
- ✅ 4xx/5xx errors
- ✅ Unusual activity patterns

**What NOT to log:**
- ❌ Passwords or tokens
- ❌ Full request/response bodies
- ❌ Personal information
- ❌ Session secrets

## Security Checklist

### Pre-Deployment

- [ ] SESSION_SECRET is strong and unique
- [ ] All secrets in Secret Manager
- [ ] HTTPS enforced
- [ ] Security headers configured
- [ ] Rate limiting enabled
- [ ] CORS properly configured
- [ ] Dependencies up to date
- [ ] No secrets in code or logs
- [ ] Error messages don't expose internals
- [ ] Input validation implemented

### Production Monitoring

- [ ] Set up security alerts
- [ ] Monitor error rates
- [ ] Track rate limit violations
- [ ] Review logs regularly
- [ ] Rotate secrets quarterly
- [ ] Update dependencies monthly
- [ ] Test incident response
- [ ] Backup configuration

## Security Contacts

**Report security vulnerabilities:**
- GitHub Security Advisories (private disclosure)
- Or email: security@yourproject.com (set this up)

**Response time:**
- Critical: 24 hours
- High: 7 days
- Medium: 30 days

## Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Google Cloud Security Best Practices](https://cloud.google.com/security/best-practices)
- [Last.fm API Security](https://www.last.fm/api/authentication)
- [OAuth 2.0 Security](https://oauth.net/2/security/)

## Updates

This document should be reviewed and updated:
- After each security incident
- Quarterly (minimum)
- When adding new features
- After infrastructure changes
