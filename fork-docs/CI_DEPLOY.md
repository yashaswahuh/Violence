# CI / CD & Deployment Guide

This document describes the automated GitHub Actions CI/CD workflows for the Violence repository.

---

## 1. Workflows Overview

- **Web Deployment (`.github/workflows/web.yml`)**:
  - Automatically triggered on push to `violence-main` when changes occur under `web/**` or the workflow itself, or via manual `workflow_dispatch`.
  - Packages the companion browser extension, builds the React 19 web player with base path `/violence/`, and publishes static output (`web/app/dist`) directly to GitHub Pages.
  - Configures client-side SPA routing (`404.html` deep link redirect restoring into `index.html`), PWA manifest (`/violence/manifest.json`), robots disallow, and posts the deployed URL to the job summary.

- **Android Phone & TV Debug Builds (`.github/workflows/android.yml`)**:
  - Automatically triggered on push to `violence-main` when Android/shared code changes (ignoring docs and web), or via manual `workflow_dispatch`.
  - Runs with JDK 17 and runner Android SDK; compiles Phone (`:app:assembleDevDebug`) and TV (`:tv:assembleDevDebug`) APKs without touching frozen modules (`-Pbitchord.bundleWear=false`).
  - Renames outputs to `violence-phone-debug-<sha>.apk` and `violence-tv-debug-<sha>.apk` and uploads them with 30-day artifact retention for collaborators.

---

## 2. One-Time Manual Repository Setup

1. **Enable GitHub Pages via Actions**:
   - Navigate to repository **Settings** ➔ **Pages**.
   - Under **Build and deployment** ➔ **Source**, select **GitHub Actions** (do NOT choose "Deploy from a branch").
2. **Enable GitHub Actions**:
   - Navigate to **Settings** ➔ **Actions** ➔ **General**.
   - Ensure **Allow all actions and reusable workflows** is selected.
   - Under **Workflow permissions**, verify "Read repository contents and packages permissions" or adjust as needed.
3. **Invite Collaborators**:
   - Navigate to **Settings** ➔ **Collaborators** ➔ **Add people**.
   - Collaborators can view GitHub Actions runs and download APK artifacts from private runs.
4. **CLI Workflow Scope**:
   - Pushing files inside `.github/workflows/` via GitHub CLI requires the `workflow` OAuth scope. Run `gh auth refresh -s workflow` once in terminal to grant it if needed before `git push`.

---

## 3. How to Trigger and Access Artifacts

- **Manual Trigger**:
  1. Open the **Actions** tab on GitHub.
  2. Select **Web Deploy** or **Android CI** in the left sidebar.
  3. Click **Run workflow**, select branch `violence-main`, and click **Run workflow**.
- **Where Outputs Appear**:
  - **Web URL**: Posted directly in the run's **Job Summary** and in the repository **Deployments** panel (`github-pages`).
  - **APK Downloads**: Listed at the bottom of the run page under **Artifacts** (`violence-phone-debug-<sha>` and `violence-tv-debug-<sha>`).

---

## 4. Troubleshooting & Verification

- **Pages Deployment Fails**: Ensure Pages source is set to "GitHub Actions" in repository settings. Verify that environment `github-pages` is created automatically.
- **Android Compilation Fails**: Check Gradle build logs. Verify that no root tasks ran and `-Pbitchord.bundleWear=false` skipped `:wearos`.
- **Signature Mismatch on Android**: Ephemeral debug signing keys change between CI runs. If updating an existing install on a test device, uninstall the older debug build first.

---

## 5. Critical Technical Notes

1. **GitHub Pages URL Visibility**: On standard GitHub accounts (including GitHub Pro), GitHub Pages sites are **publicly accessible** over the internet by URL even if the repository is private (private Pages requires GitHub Enterprise). Only share the web URL with trusted collaborators.
2. **Custom Domain Status [UNCLEAR]**: `https://yashaswahuh.is-a.dev/violence/` functions only if your GitHub user/organization Pages site (`<username>.github.io`) already configures `yashaswahuh.is-a.dev` as its verified custom domain. If not configured, the site will be served at `https://<github-username>.github.io/violence/`. Please verify this setting in your GitHub Pages user site.
3. **Companion Browser Extension Requirement**: As audited in `WEB_TARGET_AUDIT.md`, browsers block direct YouTube Music audio requests due to CORS and request headers. Users must install the companion browser extension (downloadable directly from the web player under the `/violence/extension` page) to play audio.
