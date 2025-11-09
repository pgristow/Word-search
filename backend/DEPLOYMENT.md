# Word Search Backend - Deployment Guide

This guide will help you deploy your Word Search backend to the cloud so your Android app can connect to it from anywhere.

## Option 1: Railway.app (Recommended - Easiest & Free)

Railway offers a free tier perfect for development and testing.

### Prerequisites
- GitHub account
- Railway account (sign up at https://railway.app)

### Step-by-Step Deployment

#### 1. Push Your Code to GitHub
```bash
# If you haven't already pushed to GitHub, do it now
cd backend
git add .
git commit -m "Prepare backend for deployment"
git push origin your-branch-name
```

#### 2. Deploy to Railway

1. Go to https://railway.app and sign in with GitHub
2. Click "New Project"
3. Select "Deploy from GitHub repo"
4. Choose your `Word-search` repository
5. Railway will auto-detect it's a Spring Boot app

#### 3. Add PostgreSQL Database

1. In your Railway project, click "New"
2. Select "Database" → "Add PostgreSQL"
3. Railway will automatically create a PostgreSQL database

#### 4. Configure Environment Variables

Click on your backend service, go to "Variables" tab, and add these:

```
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=${{Postgres.DATABASE_URL}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
JWT_SECRET=YourSuperSecretJWTKeyHereMakeItLongAndRandom123456789
PORT=8080
```

**Important:** Railway automatically provides `Postgres.DATABASE_URL`, `Postgres.PGUSER`, and `Postgres.PGPASSWORD` when you add PostgreSQL.

#### 5. Deploy!

Railway will automatically build and deploy your app. Watch the deployment logs.

#### 6. Get Your Backend URL

Once deployed, Railway will give you a URL like: `https://your-app-name.up.railway.app`

Copy this URL - you'll need it for the Android app!

---

## Option 2: Render.com (Also Free & Easy)

### Step-by-Step Deployment

#### 1. Sign up at https://render.com

#### 2. Create a New Web Service

1. Click "New +" → "Web Service"
2. Connect your GitHub repository
3. Select the `backend` directory
4. Fill in:
   - **Name:** word-search-backend
   - **Environment:** Docker
   - **Plan:** Free

#### 3. Add PostgreSQL Database

1. Click "New +" → "PostgreSQL"
2. Name it `word-search-db`
3. Plan: Free
4. Click "Create Database"

#### 4. Configure Environment Variables

In your Web Service settings, add these environment variables:

```
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=<copy internal database URL from PostgreSQL>
SPRING_DATASOURCE_USERNAME=<from PostgreSQL settings>
SPRING_DATASOURCE_PASSWORD=<from PostgreSQL settings>
JWT_SECRET=YourSuperSecretJWTKeyHereMakeItLongAndRandom123456789
```

#### 5. Deploy!

Render will build using the Dockerfile and deploy. First deploy takes ~5-10 minutes.

#### 6. Get Your Backend URL

Your URL will be: `https://word-search-backend.onrender.com`

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
buildConfigField("String", "BASE_URL", "\"https://your-app-name.up.railway.app/\"")
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
- Check logs: Railway/Render dashboard → Logs

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

**Railway:** Click on your service → "Deployments" → Select deployment → View logs

**Render:** Dashboard → Your service → "Logs" tab

**Fly.io:** `fly logs`

---

## Cost & Limits

### Railway Free Tier
- $5 credit per month
- Enough for 500 hours of running time
- Perfect for development/testing

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
- [ ] Enable HTTPS (automatic on Railway/Render/Fly)
- [ ] Set up monitoring/alerts
- [ ] Review logs configuration
- [ ] Test all API endpoints
- [ ] Load test with expected traffic

---

## Need Help?

- Railway Docs: https://docs.railway.app
- Render Docs: https://render.com/docs
- Fly.io Docs: https://fly.io/docs
- Spring Boot Docs: https://spring.io/guides
