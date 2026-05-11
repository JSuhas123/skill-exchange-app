# Production Deployment Checklist

**Application:** SkillExchange Android  
**Target Platform:** Google Play Store  
**Target Users:** Rural skill-sharing community  
**Estimated Deployment Timeline:** 2-3 weeks  
**Document Version:** 1.0

---

## Phase 1: Pre-Deployment Validation (Week 1)

### Code Quality & Testing
- [ ] Unit test coverage ≥ 80% (run `./gradlew test`)
- [ ] Compose UI tests passing for all 6 refactored screens
- [ ] Android instrumentation tests passing
- [ ] No compiler warnings or errors
- [ ] No ProGuard configuration errors
- [ ] All security rules reviewed and approved by 2+ team members
- [ ] No hardcoded credentials in source code (run `git log -p | grep -i "password\|token\|secret"`)
- [ ] No debug logs with sensitive data (run `grep -r "Timber.e.*password\|Timber.d.*token" app/`)

### Code Review
- [ ] All code changes reviewed and approved
- [ ] Architecture patterns consistent with MVVM + Repository
- [ ] Error handling implemented uniformly
- [ ] Offline-first patterns applied
- [ ] Memory leak prevention verified

### Firebase Configuration
- [ ] All 7 Firestore indexes deployed to staging
- [ ] Security rules tested with mock data
- [ ] Storage rules tested with various file types/sizes
- [ ] Firestore backup enabled in Firebase Console
- [ ] Crashlytics collection configured (debug=false)
- [ ] Analytics collection configured
- [ ] Quotas set (see Firebase Audit Report)

### Dependency Check
- [ ] No deprecated dependencies (run `./gradlew dependencyUpdates`)
- [ ] All dependencies from trusted sources
- [ ] Firebase SDK versions pinned (v33.6.0)
- [ ] Kotlin version consistent (2.0.21)

### Documentation
- [ ] Firebase Audit Report reviewed
- [ ] App Stability Guidelines distributed to team
- [ ] Offline Support Guide documented
- [ ] Release Build Optimization verified
- [ ] Production Recommendations documented
- [ ] Deployment runbook created (this document)

---

## Phase 2: Staging Deployment (Week 1-2)

### Staging Environment Setup
- [ ] Create staging Firebase project
- [ ] Deploy staging Firestore rules
- [ ] Deploy staging storage rules
- [ ] Configure staging analytics property
- [ ] Create 50 staging test accounts
- [ ] Generate staging test data (1000 posts, 500 users)

### Build & Deployment
- [ ] Create signed release APK
- [ ] Verify APK size < 50MB
- [ ] Upload APK to Firebase App Distribution
- [ ] Configure over-the-air (OTA) update testing
- [ ] Generate ProGuard mapping file and store securely

### Staging Testing (48 hours minimum)

#### Automated Testing
- [ ] Run full test suite against staging Firebase
- [ ] Load test with 100 concurrent users (see load testing guide)
- [ ] Security scan for common vulnerabilities
- [ ] Performance baseline on Pixel 3/4/5 devices

#### Manual Testing
- [ ] **Authentication Flow**
  - [ ] Anonymous sign-in works
  - [ ] Session persists after app restart
  - [ ] Session recovers from network interruption
  - [ ] Auto-logout after 24 hours (if implemented)

- [ ] **Create Post Flow**
  - [ ] Form validation works for all fields
  - [ ] Error messages are user-friendly
  - [ ] Post appears in feed immediately
  - [ ] Offline: post queued and syncs when online

- [ ] **Swap Initiation**
  - [ ] Duplicate swap prevention works (deduplication test)
  - [ ] Confirmation dialog appears
  - [ ] Transaction atomic (both users' scores updated)
  - [ ] Idempotent if retried

- [ ] **Messaging**
  - [ ] Message ordering preserved across 10+ messages
  - [ ] Message timestamps correct
  - [ ] Offline message queueing works
  - [ ] Message delivery indicator shows

- [ ] **Profile Management**
  - [ ] Profile image upload works (various sizes)
  - [ ] Image compression applied (60% quality)
  - [ ] 5MB limit enforced
  - [ ] Offline: image queued for upload

- [ ] **Network Scenarios**
  - [ ] WiFi toggle: app handles gracefully
  - [ ] Mobile toggle: app handles gracefully
  - [ ] Airplane mode toggle: app handles gracefully
  - [ ] Offline → Online transition: automatic sync
  - [ ] Slow network (throttle to 2G): app still responsive

- [ ] **Device Scenarios**
  - [ ] Small phones (4.5" screen)
  - [ ] Tablets (10" screen)
  - [ ] Various Android versions (7.0, 10, 12, 15)
  - [ ] High/Low memory devices
  - [ ] Dark mode toggle

- [ ] **Performance**
  - [ ] App startup < 3 seconds
  - [ ] Post list scrolls smoothly (60 FPS)
  - [ ] Memory usage < 150MB at rest
  - [ ] No ANR (Application Not Responding) errors
  - [ ] Battery drain < 5% per hour at rest

#### Crash Testing
- [ ] Monitor Crashlytics dashboard
- [ ] Zero critical crashes during 48-hour test
- [ ] All errors properly logged with context
- [ ] Crash rate < 0.5%

### Staging Sign-Off
- [ ] Product owner approves feature completeness
- [ ] QA lead signs off on test coverage
- [ ] Security team approves Firebase rules
- [ ] DevOps approves deployment configuration

---

## Phase 3: Production Deployment (Week 2-3)

### Firebase Production Setup
- [ ] Create production Firebase project
- [ ] Backup production Firestore data (if migrating from dev)
- [ ] Deploy production Firestore rules

```bash
# Deploy to production (must have editor role)
firebase deploy --only firestore:rules \
  --project skillexchange-prod
firebase deploy --only firestore:indexes \
  --project skillexchange-prod
firebase deploy --only storage:rules \
  --project skillexchange-prod
```

- [ ] Deploy storage rules
- [ ] Deploy indexes (7 composite indexes)
- [ ] Enable Firestore backup (daily at 2 AM UTC)
- [ ] Enable Crashlytics production collection
- [ ] Enable Analytics production collection
- [ ] Set Firebase quotas (see Firebase Audit Report)
- [ ] Create monitoring alerts for key metrics

### Google Play Store Setup
- [ ] Create app listing in Google Play Console
- [ ] Upload privacy policy (required by Google Play)
- [ ] Upload content rating questionnaire
- [ ] Configure app category: "Social"
- [ ] Configure content rating: "3+" (no mature content)
- [ ] Setup store listing graphics
- [ ] Configure IAP (if using paid features)
- [ ] Create internal testing track in Google Play

### Internal Testing Track (Phase 3a: 1 week)
- [ ] Upload signed APK to internal testing track
- [ ] Add 20 internal testers (team members)
- [ ] Test for 7 days minimum
- [ ] Monitor Crashlytics for production crashes
- [ ] Monitor Analytics for event flow
- [ ] Fix any critical issues found

### Beta Testing Track (Optional, Phase 3b: 1 week)
- [ ] Upload signed APK to beta testing track
- [ ] Add 100 beta testers (external testers)
- [ ] Test for 7 days minimum
- [ ] Gather feedback via in-app surveys
- [ ] Monitor crash logs and fix issues
- [ ] Prepare release notes

### Production Deployment (Phase 3c)
- [ ] Final code review sign-off
- [ ] Final Firebase rules validation
- [ ] Upload APK to production track
- [ ] Create release notes (visible to users)
- [ ] Set rollout percentage (start at 10% for 4 hours, then 100%)
- [ ] Have rollback plan ready

---

## Phase 4: Post-Deployment Monitoring (First 48 hours)

### Monitoring & Observability
- [ ] Crashlytics dashboard monitored every 15 minutes
- [ ] Analytics dashboard shows expected event volume
- [ ] Firebase quota alerts not triggered
- [ ] App performance metrics within targets
- [ ] User feedback channels active (Twitter, support email)
- [ ] Runbook accessible to all on-call engineers

### Key Metrics to Watch
| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| Crash Rate | < 0.5% | > 2% |
| ANR Rate | < 0.1% | > 0.5% |
| App Startup | < 3s | > 5s |
| Memory (Avg) | < 150MB | > 200MB |
| Firestore Reads/sec | < 10,000 | > 15,000 |
| Error Messages | < 1% | > 5% |

### Daily Checks (First Week)
- [ ] Day 1: Download count, crash rate, top issues
- [ ] Day 2: User feedback, top crashes, performance
- [ ] Day 3: Error patterns, resource usage
- [ ] Day 4: Stability trend, any new crashes
- [ ] Day 5: Confirm no critical issues
- [ ] Day 6-7: Monitor for edge cases

### Runbook for Issues

#### If Crash Rate > 2%
1. Immediately pause rollout in Google Play (set to 0%)
2. Check Crashlytics for top crash
3. Investigate root cause
4. Create hotfix patch
5. Redeploy with fix
6. Resume rollout at 10%

#### If Firestore Quota Exceeded
1. Check Firebase quota dashboard for spike cause
2. Verify no infinite loops in repositories
3. Check for unexpected traffic pattern
4. Increase quotas temporarily
5. Create ticket to optimize queries

#### If Users Can't Sign In
1. Check Firebase authentication status page
2. Verify Firebase project is accessible
3. Check network connectivity in app
4. Restart Firebase emulator if testing
5. Contact Firebase support

---

## Phase 5: Stabilization (Days 3-7)

### Monitoring Continues
- [ ] Daily trend analysis of metrics
- [ ] User feedback categorization and prioritization
- [ ] Performance optimization if needed
- [ ] Security patches if vulnerabilities found
- [ ] Documentation updates based on learnings

### Post-Launch Analysis
- [ ] Collect crash reports and categorize
- [ ] Gather user feedback and prioritize features
- [ ] Identify performance bottlenecks
- [ ] Plan next release with learnings
- [ ] Hold post-mortem if any critical issues

### Scale Preparation
- [ ] Prepare for 10x user growth
- [ ] Verify Firestore sharding strategy
- [ ] Plan infrastructure scaling
- [ ] Create user support workflows
- [ ] Setup customer feedback collection

---

## Phase 6: Long-Term Support

### Weekly Checks (After First Week)
- [ ] Monitor Crashlytics trends
- [ ] Crash rate trending down?
- [ ] Performance metrics stable?
- [ ] New errors appearing?
- [ ] User feedback sentiment?

### Monthly Updates
- [ ] Collect user feedback
- [ ] Plan next features
- [ ] Security updates and patches
- [ ] Performance optimizations
- [ ] Firebase quota reviews

### Quarterly Reviews
- [ ] User growth metrics
- [ ] Feature usage analytics
- [ ] Infrastructure scaling needs
- [ ] Architecture improvements
- [ ] Security audit

---

## Rollback Procedure

If production deployment must be rolled back:

### Option 1: Gradual Rollback (Preferred)
```bash
# In Google Play Console:
# 1. Go to Release Management → Releases
# 2. Set rollout percentage to 0%
# 3. Wait 5 minutes for users to be removed
# 4. Users will revert to previous version on next update check
```

### Option 2: Emergency Hotfix
```bash
# 1. Create fix branch from production tag
# git checkout -b hotfix/critical-crash v1.0.0
# 2. Implement fix with minimal changes
# 3. Test thoroughly
# 4. Tag as v1.0.1
# 5. Build, sign, and upload APK to Google Play
# 6. Set rollout to 100% (urgent)
```

### Option 3: Server-Side Disable (If Firebase Issue)
```bash
# In Firebase Console:
# 1. Create Remote Config key: "app_min_version" = "999.0.0"
# 2. Users with version < min_version see "Please update" dialog
# 3. Prevents old app from accessing services
```

---

## Deployment Team & Responsibilities

| Role | Responsibility | Contact |
|------|-----------------|---------|
| Release Manager | Coordinate timeline, Google Play submission | TBD |
| Platform Engineer | Firebase setup, CI/CD, monitoring | TBD |
| QA Lead | Testing coordination, sign-off | TBD |
| DevOps | Deployment automation, alerts | TBD |
| On-Call Engineer | Monitor first 48 hours, handle incidents | TBD |
| Product Manager | Feature validation, release notes | TBD |

---

## Communication Plan

### Before Deployment
- [ ] Team meeting: Deploy day walkthrough
- [ ] Stakeholder email: Release date and expected features
- [ ] Support team: Update FAQs and support docs

### During Deployment
- [ ] Slack channel: #skillexchange-deployment (live updates)
- [ ] Status page: Public status.skillexchange.app (real-time)
- [ ] Email: Stakeholders with status updates every 2 hours

### After Deployment
- [ ] Blog post: "SkillExchange v1.0.0 Released"
- [ ] Social media: Announcement of launch
- [ ] Email: Users with what's new
- [ ] In-app notification: Welcome message for new users

---

## Success Criteria

**Deployment is considered successful if:**
- ✅ 95% of users installed app without errors
- ✅ Crash rate < 0.5% on Day 1
- ✅ Crash rate < 0.2% by Day 7
- ✅ All critical features working (auth, posts, swaps, messaging)
- ✅ No security vulnerabilities exploited
- ✅ Performance metrics within targets
- ✅ User satisfaction > 4.0 stars (in reviews)
- ✅ Support team receives < 5 critical issues

---

## Appendix A: Pre-Deployment Commands

```bash
# 1. Clean build
./gradlew clean

# 2. Run all tests
./gradlew test
./gradlew connectedAndroidTest

# 3. Build release APK
./gradlew assembleRelease

# 4. Verify APK size
ls -lh app/build/outputs/apk/release/app-release.apk

# 5. Verify signing
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# 6. Deploy Firebase rules
firebase deploy --only firestore:rules,firestore:indexes,storage:rules

# 7. Deploy to Google Play Internal Testing
bundletool build-apks --bundle=app-release.aab \
  --output=app.apks \
  --ks=release.keystore \
  --ks-key-alias=skillexchange

# 8. Monitor Crashlytics
firebase crashlytics:symbols:upload app/build/intermediates/cppSymbols/release/
```

---

## Appendix B: Firebase Rules Summary

- ✅ 6 collections protected (users, skills, posts, swaps, messages, exchangeRequests)
- ✅ All writes validated
- ✅ All reads authenticated
- ✅ Owner-only write enforcement
- ✅ Transaction atomicity for swaps
- ✅ No wildcard permissions

---

## Appendix C: Monitoring Dashboards to Setup

1. **Crashlytics Dashboard**
   - Crash-free users metric
   - Top crashing issues
   - Crash trends over time

2. **Analytics Dashboard**
   - Active users DAU/MAU
   - Event flow (sign-ins, posts created, messages sent)
   - User retention D1/D7/D30

3. **Firebase Performance**
   - App startup time distribution
   - Screen load time
   - HTTP request latency

4. **Google Play Console**
   - Install count trend
   - Uninstall rate
   - Rating trend
   - Reviews sentiment

---

**Document Owner:** Release Engineering  
**Created:** 2025-01-15  
**Last Updated:** 2025-01-15  
**Next Review:** 2025-02-15 (post-launch review)
