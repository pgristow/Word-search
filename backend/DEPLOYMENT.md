# Word Search Backend - Deployment Guide

This guide will help you deploy your Word Search backend to the cloud so your Android app can connect to it from anywhere.

## Option 1: Render (Recommended - Free)

The repo ships a `render.yaml` Blueprint that provisions the Dockerized backend **and** a
free Postgres in one step. (See the root `DEPLOYMENT.md` for the full write-up.)

### Easiest: Blueprint
1. Push your code to GitHub (already connected).
2. In Render: **New → Blueprint**, select the `Word-search` repo.
3. Render reads `render.yaml` and creates `wordsearch-db` (Postgres) + `wordsearch-backend`
   (web service from `backend/Dockerfile`). Click **Apply**.
4. DB env vars (`DB_HOST`/`DB_PORT`/`DB_NAME`/username/password), a generated `JWT_SECRET`,
   and `SPRING_PROFILES_ACTIVE=production` are injected automatically. Flyway runs `V1..V6`
   on first boot.
5. Render gives you a URL like `https://wordsearch-backend.onrender.com`.

### Manual (without the Blueprint)
1. **New → Web Service**, connect the repo, Environment **Docker**, Plan **Free**.
2. **New → PostgreSQL**, Plan **Free**.
3. On the web service, set env vars:
   ```
   SPRING_PROFILES_ACTIVE=production
   DB_HOST=<from the Postgres "Connections" panel>
   DB_PORT=5432
   DB_NAME=<database name>
   SPRING_DATASOURCE_USERNAME=<user>
   SPRING_DATASOURCE_PASSWORD=<password>
   JWT_SECRET=<a long random secret>
   ```

> Render's free Postgres is deleted ~30 days after creation; for a permanent free DB use
> Neon (neon.tech) and point the same `DB_*` vars at it.

---

## Option 3: Fly.io (Free Tier Available)

### Quick Setup

```bash
# Install Fly CLI
curl -L https://fly.io/install.sh | sh

# Login
fly auth login

# Deploy
cd backend
fly launch

# Create PostgreSQL
fly postgres create

# Attach database
fly postgres attach <postgres-app-name>

# Set environment variables
fly secrets set JWT_SECRET=YourSuperSecretJWTKeyHereMakeItLongAndRandom123456789
fly secrets set SPRING_PROFILES_ACTIVE=production

# Deploy
fly deploy
```

---

## Update Android App to Use Deployed Backend

Once you have your backend URL, update the Android app:

### Edit `android/app/build.gradle.kts`

Find this line (around line 26):
```kotlin
buildConfigField("String", "BASE_URL", "\"http://172.23.59.85:8080/\"")
```

Replace with your deployed URL:
```kotlin
buildConfigField("String", "BASE_URL", "\"https://wordsearch-backend.onrender.com/\"")
```

### Rebuild the Android App

```bash
cd android
./gradlew clean build
```

---

## Verifying Deployment

### Test Your Backend

```bash
# Health check
curl https://your-app-url.com/actuator/health

# Should return: {"status":"UP"}
```

### Test Registration

```bash
curl -X POST https://your-app-url.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!",
    "confirmPassword": "Password123!"
  }'
```

---

## Troubleshooting

### Common Issues

**1. Database Connection Errors**
- Check that `SPRING_DATASOURCE_URL` is set correctly
- Verify PostgreSQL is running
- Check logs: Render dashboard → Logs

**2. JWT Secret Error**
- Make sure `JWT_SECRET` environment variable is set
- Must be at least 32 characters long

**3. Build Failures**
- Check Dockerfile is present
- Verify Java version (should be 17)
- Check logs for specific error

**4. App Won't Start**
- Check that `SPRING_PROFILES_ACTIVE=production` is set
- Verify all environment variables are configured
- Check logs for errors

### View Logs

**Render:** Dashboard → Your service → "Logs" tab

**Fly.io:** `fly logs`

---

## Cost & Limits

### Render Free Tier
- 750 hours per month
- Spins down after 15 minutes of inactivity
- First request after sleep takes ~30 seconds

### Fly.io Free Tier
- 3 shared-cpu-1x VMs
- 160GB outbound data transfer
- Always running (no sleep)

---

## Production Checklist

Before going to production:

- [ ] Change `JWT_SECRET` to a strong, random value
- [ ] Set up proper database backups
- [ ] Configure CORS for your domain
- [ ] Enable HTTPS (automatic on Render/Fly)
- [ ] Set up monitoring/alerts
- [ ] Review logs configuration
- [ ] Test all API endpoints
- [ ] Load test with expected traffic

---

## Need Help?

- Render Docs: https://render.com/docs
- Fly.io Docs: https://fly.io/docs
- Spring Boot Docs: https://spring.io/guides
