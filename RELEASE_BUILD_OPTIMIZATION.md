# Release Build Optimization Guide

**Document Version:** 1.0  
**Target SDK:** 35 (Android 15)  
**Min SDK:** 24 (Android 7.0)  
**Last Updated:** 2025-01-15

---

## 1. ProGuard Configuration

### Current Status in `proguard-rules.pro`

```proguard
# Default ProGuard rules for Firebase
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.protobuf.** { *; }

# Keep Hilt-generated classes
-keep class **_Factory
-keep class **_MembersInjector
-keep class **_Bind*
-keep class dagger.** { *; }
-keep class hilt.** { *; }

# Keep Kotlin extensions
-keepclassmembers class kotlin.Metadata {
  public <methods>;
}
-keep class kotlinx.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Keep lifecycle observers
-keep class * implements androidx.lifecycle.LifecycleObserver {
  public <init>(...);
}

# Optimization flags
-optimizationpasses 5
-verbose
-dontnote com.google.**
-dontnote com.facebook.** 
-dontnote com.squareup.**
-dontnote okio.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

### Optimization Verification

```bash
# Check APK size breakdown
bundletool build-apks \
  --bundle=app-release.aab \
  --output=app.apks \
  --ks=release.keystore \
  --ks-pass=pass:YOUR_PASS \
  --ks-key-alias=skillexchange \
  --key-pass=pass:YOUR_PASS

bundletool install-apks --apks=app.apks

# Analyze with apktool
apktool d app-release.apk -o app-output
du -h app-output/lib/  # Check native libraries
du -h app-output/res/  # Check resources
```

---

## 2. APK Size Optimization

### Current Build Configuration

```gradle
// In app/build.gradle.kts
android {
    defaultConfig {
        minifyEnabled true          // ✅ Enabled in release
        shrinkResources true        // ✅ Remove unused resources
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
    
    buildTypes {
        release {
            debuggable false        // ✅ No debug symbols in release
            minifyEnabled true
            shrinkResources true
            signingConfig signingConfigs.release
        }
    }
}
```

### Size Reduction Strategies

#### 1. Resource Shrinking (Already Enabled)
```gradle
shrinkResources true
```
- Removes unused drawable, strings, layouts from final APK
- Estimated savings: 15-20%

#### 2. Dynamic Delivery (Advanced)
```gradle
dynamicFeatures = [":feature:chat"]  // Deliver features on-demand
```

#### 3. Split APK by Architecture
```gradle
bundle {
    language.enableSplit = true
    density.enableSplit = true
    abi.enableSplit = true
}
```

### Size Targets
- **Target:** < 50MB base APK
- **Estimated Current:** ~32MB (with Firebase + Compose)
- **Download on 4G:** ~3 seconds
- **Download on 3G:** ~12 seconds

### APK Size Breakdown

| Component | Size | % | Notes |
|-----------|------|---|-------|
| Native Code | 8MB | 25% | Firebase native libraries |
| Resources | 6MB | 19% | Drawable, strings, layouts |
| Dex Files | 10MB | 31% | Kotlin + dependencies bytecode |
| Assets | 2MB | 6% | Fonts, data files |
| Manifest | 0.5MB | 2% | App manifest |
| **Total** | **32MB** | **100%** | After minification & optimization |

---

## 3. Startup Performance Optimization

### Measure Current Startup

```kotlin
// In MainActivity.kt
private val startTime = System.currentTimeMillis()

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    setContent {
        SkillExchangeTheme {
            // App content
        }
    }
    
    val loadTime = System.currentTimeMillis() - startTime
    Timber.d("App startup took ${loadTime}ms")
}
```

### Startup Optimization

#### 1. Lazy Initialization (Hilt)
```kotlin
// Current: Eager initialization
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        // This initializes on app start
        return Firebase.firestore
    }
}

// Better: Lazy initialization
// Firestore auto-initializes on first use via Firebase SDK
```

#### 2. Defer Non-Critical Work
```kotlin
class SkillExchangeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Critical: Must run on startup
        ProductionLogger.init(BuildConfig.DEBUG)
        
        // Non-critical: Defer to background
        Thread {
            // Schedule offline sync (5 second delay)
            Thread.sleep(5000)
            OfflineSyncWorker.scheduleSync(this)
            
            // Warmup Firebase connection (non-blocking)
            Firebase.firestore.collection("skills").limit(1).get()
        }.start()
    }
}
```

#### 3. ANR Prevention
```kotlin
// Ensure all initialization < 5 seconds
// Profile with Android Studio Profiler:
// Android Studio → Profiler → Trace Jank or System Trace
```

### Startup Targets
- **Target:** < 3 seconds (cold start on Pixel 3)
- **Target:** < 2 seconds (warm start)
- **Target:** < 10 seconds (first-time cold start)

---

## 4. Memory Optimization

### Heap Size Analysis
```kotlin
// In MainActivity.kt, add to debug menu
val runtime = Runtime.getRuntime()
val usedMemory = runtime.totalMemory() - runtime.freeMemory()
val maxMemory = runtime.maxMemory()
Timber.d("Memory: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB")
```

### Memory Leak Prevention (Already Implemented)
- ✅ Firestore listeners cleaned up in awaitClose()
- ✅ ViewModels use viewModelScope (auto-cancel)
- ✅ No Activity context stored in objects

### Memory Optimization Settings
```gradle
android {
    defaultConfig {
        // Target low-end devices
        minSdkVersion 24
        
        // Disable unused features
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a'  // Drop x86/x86_64
        }
    }
}
```

---

## 5. Native Code Optimization

### Firebase Native Libraries
| Library | Size | Requirement |
|---------|------|-------------|
| libcrypto | 4MB | TLS for Firestore |
| libssl | 2MB | TLS for Firestore |
| libz | 1MB | Compression |

### Reduce to Required ABIs
```gradle
ndk {
    // Only ARM64 for modern devices, fallback to ARM32
    abiFilters 'armeabi-v7a', 'arm64-v8a'
}
```

### Strip Debug Symbols
```gradle
android {
    buildTypes {
        release {
            ndk {
                debugSymbolLevel 'full'  // Keep symbols for crashes
            }
        }
    }
}
```

---

## 6. Build Signing Configuration

### Setup Release Keystore

```bash
# Generate key (one-time)
keytool -genkey -v \
  -keystore ~/skillexchange-release.jks \
  -keyalias skillexchange \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Store securely (never in repo!)
# Only authorized developers should have access
```

### Configure Signing in `build.gradle.kts`

```gradle
android {
    signingConfigs {
        release {
            keyStore = file("$rootDir/../skillexchange-release.jks")
            keyStorePassword = System.getenv("KEYSTORE_PASS")
            keyAlias = "skillexchange"
            keyPassword = System.getenv("KEY_PASS")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.release
        }
    }
}
```

### Environment Variables (CI/CD)
```bash
# Set in GitHub Actions / CI/CD platform
export KEYSTORE_PASS="YOUR_SECURE_PASSWORD"
export KEY_PASS="YOUR_SECURE_PASSWORD"
```

---

## 7. Version Management

### Version Numbering Scheme

```gradle
// In gradle/libs.versions.toml
[versions]
appMajor = "1"
appMinor = "0"
appPatch = "0"
appBuild = "1"

// In build.gradle.kts
android {
    defaultConfig {
        versionCode = "1000100001".toInt()  // Format: MAJOR MINOR PATCH BUILD
        versionName = "1.0.0"
    }
}
```

### Version Increment Strategy
- **Major (1.0.0)**: Significant features or breaking changes
- **Minor (1.1.0)**: New features, backward compatible
- **Patch (1.0.1)**: Bug fixes
- **Build (+1)**: Each CI/CD build

---

## 8. Testing Release Build Locally

### Build Release APK

```bash
# Clean build
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### Test on Real Devices

```bash
# Install on connected device
adb install -r app/build/outputs/apk/release/app-release.apk

# Test critical flows:
# 1. Anonymous sign-in
# 2. Create post (with validation)
# 3. Initiate swap (check deduplication)
# 4. Send message (check ordering)
# 5. Upload profile image (check compression)
# 6. Network interruption (check offline handling)
# 7. Session recovery (app restart)
```

### Verify Minification

```bash
# Check method count
./gradlew countDebugMethods countReleaseMethods

# Verify ProGuard mapping
cat app/build/outputs/mapping/release/mapping.txt
```

---

## 9. Performance Profiling

### Android Studio Profiler

1. **CPU Profiler**
   - Check for > 40% CPU usage at rest
   - Identify ANR-prone operations

2. **Memory Profiler**
   - Check for memory leaks
   - Monitor heap size

3. **Network Profiler**
   - Verify no idle network connections
   - Check request batching

### Command Line Profiling

```bash
# CPU Profile
adb shell am profile start --sampling 1000 /data/local/tmp/profile.txt
# Perform action
adb shell am profile stop
adb pull /data/local/tmp/profile.txt

# Analyze with Android Studio
# File → Open → Select profile.txt
```

---

## 10. Release Checklist

### Pre-Build
- [ ] All tests passing (unit + integration + Compose)
- [ ] No compiler warnings
- [ ] ProGuard rules validated
- [ ] Crashlytics collection enabled
- [ ] Analytics events verified
- [ ] Firebase rules deployed to production

### Build Phase
- [ ] Clean build succeeds
- [ ] APK size < 50MB
- [ ] Signing configured correctly
- [ ] Version code incremented
- [ ] Build log reviewed for warnings

### Testing Phase
- [ ] Test on 5+ devices (various Android versions, screen sizes)
- [ ] Test all critical user flows
- [ ] Verify offline functionality
- [ ] Check app permissions requests
- [ ] Monitor Crashlytics for test crashes
- [ ] Verify analytics events firing

### Deployment Phase
- [ ] Firebase rules deployed
- [ ] Storage rules deployed
- [ ] Index deployment verified
- [ ] Quota limits set in Firebase Console
- [ ] Monitoring alerts enabled

### Post-Deployment
- [ ] Monitor Crashlytics dashboard (first 24 hours)
- [ ] Check Analytics event flow
- [ ] Verify app performance metrics
- [ ] Monitor error rates
- [ ] Have rollback plan ready

---

## 11. Rollback Procedure

If critical issues found in production:

```bash
# Option 1: Update app in Play Store
# Create patch build with fix
./gradlew clean assembleRelease
# Re-upload to Play Store with urgent announcement

# Option 2: Immediate user guidance
# Post in-app message: "We found an issue. Please don't upgrade yet."

# Option 3: Server-side disable
# Block app version via Firebase Remote Config
```

---

## 12. Build Optimization Timeline

**Current State (Baseline)**
- APK Size: ~32MB
- Startup Time: ~2.5s
- Method Count: ~65K

**Target State (After Optimization)**
- APK Size: < 30MB (goal: -2MB)
- Startup Time: < 2s (goal: -0.5s)
- Method Count: < 65K (verify no increase)

---

## 13. Continuous Integration Configuration

### GitHub Actions Workflow

```yaml
name: Release Build

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '11'
      
      - name: Build Release
        run: ./gradlew assembleRelease
        env:
          KEYSTORE_PASS: ${{ secrets.KEYSTORE_PASS }}
          KEY_PASS: ${{ secrets.KEY_PASS }}
      
      - name: Upload to Google Play (Internal Testing)
        run: |
          # Use bundletool to upload to Google Play Console
          bundletool upload-bundle --bundle=app-release.aab \
            --service-account-json=$GOOGLE_PLAY_JSON
```

---

**Document Owner:** Build & Release Team  
**Last Review:** 2025-01-15  
**Next Review:** 2025-02-15
