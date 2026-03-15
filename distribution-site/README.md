# SOME 배포 사이트
이 폴더는 SOME APK를 내려받는 정적 배포 사이트입니다.

## 포함 파일
- `index.html`: 사용자가 여는 다운로드 페이지
- `latest.json`: 앱이 읽는 최신 버전 메타데이터

## 생성 방법
GitHub Actions에서는 `scripts/prepare-distribution-site.ps1` 가 다음 작업을 자동으로 처리합니다.

1. `version.properties` 에서 버전 코드와 버전명을 읽습니다.
2. `app/build/outputs/apk/debug/app-debug.apk` 를 `distribution-site/apk/` 아래로 복사합니다.
3. `latest.json` 의 `versionCode`, `versionName`, `apkUrl`, `pageUrl`, `message` 를 갱신합니다.
4. `index.html` 의 표시 버전과 다운로드 링크를 갱신합니다.

로컬에서도 같은 스크립트를 직접 실행할 수 있습니다.

```powershell
./scripts/prepare-distribution-site.ps1 -BaseUrl "https://your-domain.example/"
```

## 앱 업데이트 연결
앱에서 새 버전 확인을 활성화하려면 `local.properties` 에 아래 값을 넣습니다.

```properties
update.feedUrl=https://your-domain.example/latest.json
update.siteUrl=https://your-domain.example/
```

## 주의
- 일반 Android 앱은 웹에서 배포한 APK를 완전 자동 설치할 수 없습니다.
- 앱이 새 버전을 감지하고 다운로드까지 진행할 수는 있지만, 마지막 설치 승인 단계는 사용자가 직접 눌러야 합니다.
