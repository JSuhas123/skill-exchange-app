# SkillExchange Troubleshooting Guide

## Common Issues & Solutions

### 1. App Crashes on Startup

**Symptoms**: App closes immediately after opening

**Solutions**:
1. **Clear App Cache**
   - Settings → Apps → Skill Exchange → Storage → Clear Cache
   - Wait 10 seconds and restart app

2. **Clear App Data** (if cache clear doesn't work)
   - Settings → Apps → Skill Exchange → Storage → Clear Data
   - Reinstall app - Settings → Apps → Skill Exchange → Uninstall
   - Reinstall from Google Play Store

3. **Update Android System**
   - Check Settings → System Update for pending updates
   - Some firebase versions need Android 10+

4. **Uninstall Old Version**
   - If upgrading from v1.0, uninstall completely first
   - Then install v2.0

### 2. "User not authenticated" Error

**Symptoms**: Login screen appears but keeps showing authentication error

**Solutions**:
1. Check internet connection (WiFi or cellular)
2. Try again in 30 seconds (phone verification cooldown)
3. Ensure phone number has country code (e.g., +1-234-567-8900)
4. Try again with different phone number

### 3. Slow Loading on First Launch

**Normal Behavior**: First launch takes 2-3 seconds
- Firebase initialization happens once
- Subsequent launches <1 second

**If still slow after 30 seconds**:
- Force close app: Settings → Apps → Skill Exchange → Force Stop
- Reopen app
- Check internet connection speed

### 4. Chat Messages Not Sending

**Symptoms**: Message stays in "sending" state

**Solutions**:
1. Check internet connection
2. Switch between WiFi and cellular
3. Restart app
4. Force close and reopen

**If persists**:
- Clear cache (see #1 above)
- Check if recipient is still online

### 5. Profile Photo Upload Fails

**Symptoms**: Upload button spinning or error message

**Solutions**:
1. Check file size (<5 MB)
2. Check internet connection
3. Try JPG instead of PNG
4. Clear cache and retry

### 6. Skills/Posts Not Loading

**Symptoms**: Dashboard or Skill Board shows "No posts"

**Solutions**:
1. Force refresh: Pull down on screen (if available)
2. Go to another screen and come back
3. Close app fully and reopen
4. Check internet connection

### 7. Payment/Skill Points Issues

**Symptoms**: Skills points not updating, swap doesn't complete

**Solutions**:
1. Check your internet (must be connected to process)
2. Restart app
3. Contact support with your User ID visible in Profile

---

## Advanced Troubleshooting

### Check Logs (Android Studio)

1. Connect phone via USB debugging
2. Open Android Studio > Logcat
3. Filter by "SkillExchange" or "skillexchange"
4. Share error logs with support

### Clear Firebase Cache

1. Go to Settings → Apps → Google Services
2. Tap Storage → Clear Cache
3. Reopen Skill Exchange app

### Reset All App Data

⚠️ **This will sign you out**:
1. Settings → Apps → Skill Exchange
2. Tap "Storage" → "Manage Space"
3. "Clear All Data"
4. Uninstall and reinstall app fresh

---

## When Nothing Works

1. **Restart Phone**
   - Turn off completely
   - Wait 30 seconds
   - Turn back on
   - Try app again

2. **Check Device Storage**
   - Free up at least 200 MB
   - Settings → Storage → Delete cached data
   - Uninstall unused apps

3. **Update Android**
   - Minimum Android 10 recommended
   - Android 11+ for best performance

4. **Contact Support**
   - In-app: Profile → Help & Support
   - Email: support@skillexchange.app
   - Include: device model, Android version, error message

---

## Development Setup (Local Testing)

### Build from Source

```bash
./gradlew build
```

### Run Debug APK

```bash
./gradlew installDebug
```

### View Real-Time Logs

```bash
adb logcat | grep skillexchange
```

### Test Offline Mode

1. Enable airplane mode
2. Use app normally (limited features)
3. Disable airplane mode
4. Data syncs automatically

---

## Performance Tips

- **Close Other Apps**: Frees up 100+ MB RAM
- **Disable Background Sync**: Settings → Battery → Battery Saver
- **Restart Weekly**: Clears app cache naturally
- **Use WiFi**: 2-3x faster than 4G in rural areas

---

## Known Issues

### v2.0 Release Notes
- ✅ Fixed startup crash (cache recovery)
- ✅ Fixed Firebase initialization issues  
- ✅ Fixed null pointer exceptions
- ✅ Added global error handler

### Planned Fixes (v2.1)
- Offline message queuing
- Faster image uploads
- Better low-bandwidth support

---

**Last Updated**: May 20, 2026  
**App Version**: 2.0  
**Minimum Android**: 10 (API 29)  
**Target Android**: 15 (API 35)
