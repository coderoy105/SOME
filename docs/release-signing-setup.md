# Release Signing Setup

This project now supports signed release APK deployment through GitHub Actions.

## Required GitHub repository secrets

Add these secrets in:

`GitHub repository > Settings > Secrets and variables > Actions`

- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

## How to create SIGNING_KEYSTORE_BASE64

Use the local helper script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\encode-keystore-base64.ps1 -KeystorePath "C:\path\to\some-release.jks"
```

Copy the printed output and paste it into the `SIGNING_KEYSTORE_BASE64` GitHub secret.

## Optional local.properties keys for local signed release builds

You can also build a signed release on your PC by adding these keys to `local.properties`:

```properties
signing.storeFile=C\:\\path\\to\\some-release.jks
signing.storePassword=your_store_password
signing.keyAlias=your_key_alias
signing.keyPassword=your_key_password
```

## Build commands

Local release build:

```powershell
.\gradlew.bat assembleRelease --console=plain
```

Repository deployment:

```powershell
git add .
git commit -m "Release SOME 1.0.3"
git push origin main
```

After push, GitHub Actions will build a signed release APK and deploy it to GitHub Pages.
