# 🚨 IMMEDIATE SECURITY FIX CHECKLIST

## CWE-798/CWE-259 - Hardcoded Credentials in index-05aVf9ai.js

### ⚡ URGENT ACTIONS (Do Now)

#### 1. Restrict Firebase API Key (CRITICAL)
```bash
# Go to Firebase Console immediately:
# https://console.firebase.google.com/project/afilaxy-app/settings/general
```
- Navigate to: Project Settings → General → Web API Key
- Click "Restrict Key" 
- Add HTTP referrers: `https://yourdomain.com/*`, `https://*.yourdomain.com/*`
- Enable only: Identity Toolkit API, Cloud Firestore API
- Save restrictions

#### 2. Replace Hardcoded File
```bash
# In project root directory:
cd /home/afilaxy/Projetos/afilaxy-kmm

# Run the secure build
bash build-secure.sh

# Replace the vulnerable file
mv web-dist/assets/index-05aVf9ai.js web-dist/assets/index-05aVf9ai.js.VULNERABLE.backup
```

#### 3. Update Application Source
Find the source file that generates `index-05aVf9ai.js` and replace:
```javascript
// REMOVE THIS (vulnerable):
const Dh=Jf({
  apiKey:"AIzaSyA_zwgmvohqQim5xp4IvtMe2EI7DE2ylW4",
  authDomain:"afilaxy-app.firebaseapp.com",
  projectId:"afilaxy-app",
  storageBucket:"afilaxy-app.firebasestorage.app",
  messagingSenderId:"19540410113",
  appId:"1:19540410113:web:eb0cc4543701d9f3b9e500"
});

// REPLACE WITH THIS (secure):
import firebaseConfig from './firebase.config';
const Dh = Jf(firebaseConfig);
```

### 🔧 IMPLEMENTATION STEPS

#### Step 1: Environment Setup
```bash
# Files are already created:
# ✅ web-src/firebase.config.ts (secure config)
# ✅ web-src/.env.local (environment variables)
# ✅ build-secure.sh (secure build script)
```

#### Step 2: Build Process
```bash
# Run secure build
bash build-secure.sh

# Test the application
# Verify Firebase still works
# Check browser console for errors
```

#### Step 3: Deploy Secure Version
```bash
# Deploy to Firebase Hosting
firebase deploy --only hosting

# Or your deployment method
```

### 🔍 VERIFICATION

#### Check 1: No Hardcoded Credentials
```bash
# This should return empty:
grep -r "AIzaSyA_zwgmvohqQim5xp4IvtMe2EI7DE2ylW4" web-dist/
```

#### Check 2: Environment Variables Working
```bash
# Check that config loads from environment:
grep -r "import.meta.env.VITE_FIREBASE" web-dist/
```

#### Check 3: Application Functions
- [ ] Firebase authentication works
- [ ] Firestore database access works  
- [ ] No console errors
- [ ] All features functional

### 🚨 SECURITY MONITORING

#### Set up alerts for:
- Unusual Firebase API usage
- Failed authentication attempts
- Unexpected database access patterns

#### Regular checks:
```bash
# Run security scan weekly:
bash fix-firebase-credentials.sh
```

### 📋 STATUS TRACKING

- [ ] Firebase API key restricted in console
- [ ] Hardcoded credentials removed from bundle
- [ ] Secure configuration implemented
- [ ] Application tested and working
- [ ] Secure version deployed
- [ ] Monitoring alerts configured

### 🔄 FOLLOW-UP ACTIONS

#### Within 24 hours:
- [ ] Rotate the exposed API key (generate new one)
- [ ] Update environment variables with new key
- [ ] Redeploy with new credentials

#### Within 1 week:
- [ ] Implement automated security scanning in CI/CD
- [ ] Add credential detection to pre-commit hooks
- [ ] Document secure development practices

---

**PRIORITY**: 🔴 CRITICAL - Fix immediately
**IMPACT**: Exposed Firebase credentials in public web bundle
**SOLUTION**: ✅ Ready to implement (files created above)