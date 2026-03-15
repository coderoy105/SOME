# SOME

## 1. 프로젝트 소개
SOME은 Android 15와 삼성 갤럭시 기기를 기준으로 만든 DM 답장 추천 오버레이 앱입니다.  
채팅 화면 위에 뜨는 플로팅 버블에서 사용자가 직접 화면 분석을 시작하면, 현재 보이는 대화 화면을 1회 캡처하고 OCR로 텍스트를 읽은 뒤, 상대 프로필과 말투 설정을 반영해 여러 개의 답장 추천 카드를 생성합니다.

이 프로젝트는 외부 API 키 없이 바로 실행되는 완성형 Android Studio 프로젝트입니다.

## 2. 기능 요약
- 오버레이 버블 표시, 드래그, 탭, 패널 열기
- 현재 화면 1회 캡처 후 ML Kit OCR 분석
- 최근 대화 정리, 질문형 문장 감지, 마지막 메시지 우선 추출
- 상대 프로필 저장: 이름, 관계 유형, 말투, 제한 옵션
- 내장 추천 엔진: 안전형, 재치형, 설렘형, 짧은형, 이어가기 질문형
- 추천 답장 복사, 다시 생성, 스타일 조정
- Room 기반 프로필/세션/추천 기록 저장
- 설정 화면에서 기본 관계/말투, 자동 저장, 기록 삭제, 전체 초기화
- 데모 모드 지원

## 3. 프로젝트 구조 설명
```text
SOME/
├─ app/
│  ├─ build.gradle.kts
│  ├─ proguard-rules.pro
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/example/replybubble/
│     │  ├─ data/
│     │  │  ├─ local/
│     │  │  ├─ preferences/
│     │  │  └─ repository/
│     │  ├─ di/
│     │  ├─ domain/
│     │  ├─ navigation/
│     │  ├─ ocr/
│     │  ├─ overlay/
│     │  ├─ recommendation/
│     │  ├─ service/
│     │  ├─ ui/
│     │  │  ├─ analysis/
│     │  │  ├─ common/
│     │  │  ├─ home/
│     │  │  ├─ onboarding/
│     │  │  ├─ profile/
│     │  │  ├─ recommendation/
│     │  │  ├─ settings/
│     │  │  └─ theme/
│     │  ├─ util/
│     │  ├─ MainActivity.kt
│     │  └─ ReplyBubbleApp.kt
│     └─ res/
│        ├─ drawable/
│        ├─ values/
│        ├─ values-night/
│        └─ xml/
├─ gradle/wrapper/
├─ local-maven/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ gradlew
├─ gradlew.bat
└─ README.md
```

## 4. Android Studio에서 프로젝트 여는 방법
1. Android Studio를 실행합니다.
2. `Open` 또는 `File > Open...` 메뉴를 누릅니다.
3. `C:\Users\user\Desktop\AI\some` 폴더를 선택합니다.
4. Gradle Sync가 자동으로 시작되면 완료될 때까지 기다립니다.
5. JDK는 Android Studio 기본 JDK 또는 JDK 17을 사용하면 됩니다.

## 5. 빌드 방법
### Android Studio에서 debug APK 빌드
정확한 메뉴 경로:
1. `Build`
2. `Build Bundle(s) / APK(s)`
3. `Build APK(s)`

### Gradle 명령어로 debug APK 빌드
Windows PowerShell 기준:
```powershell
set JAVA_HOME=C:\Program Files\Java\jdk-17
set ANDROID_HOME=C:\Users\user\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\user\AppData\Local\Android\Sdk
.\gradlew.bat assembleDebug
```

## 6. debug APK 생성 방법
빌드가 끝나면 다음 파일이 생성됩니다.

- `app\build\outputs\apk\debug\app-debug.apk`

이 프로젝트에서는 실제로 아래 명령으로 debug 빌드를 성공 확인했습니다.
```powershell
.\gradlew.bat assembleDebug
```

## 7. release APK 생성 방법
### Gradle 명령어로 release APK 빌드
```powershell
set JAVA_HOME=C:\Program Files\Java\jdk-17
set ANDROID_HOME=C:\Users\user\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\user\AppData\Local\Android\Sdk
.\gradlew.bat assembleRelease
```

현재 프로젝트는 기본 상태에서 unsigned release APK를 생성합니다.

생성 파일:
- `app\build\outputs\apk\release\app-release-unsigned.apk`

### Android Studio에서 signed release APK 만들기
정확한 메뉴 경로:
1. `Build`
2. `Generate Signed Bundle / APK...`
3. `APK` 선택
4. `Create new...` 로 새 keystore 생성 또는 기존 keystore 선택
5. keystore 비밀번호, key alias, key password 입력
6. `release` 변형 선택
7. `Finish`

## 8. release 서명 기본 설명
- `assembleRelease`만 실행하면 보통 `app-release-unsigned.apk`가 만들어집니다.
- 실제 배포용 설치 파일은 서명된 APK가 더 적합합니다.
- Android Studio의 `Generate Signed Bundle / APK...` 마법사를 쓰면 서명된 release APK를 만들 수 있습니다.
- 개인 테스트만 할 경우 debug APK 설치가 가장 간단합니다.

## 9. APK 생성 위치
- debug APK: `app\build\outputs\apk\debug\app-debug.apk`
- unsigned release APK: `app\build\outputs\apk\release\app-release-unsigned.apk`
- signed release APK: 보통 `app\release\` 또는 마법사에서 지정한 위치

## 10. Windows PC에서 삼성 갤럭시로 APK 옮기는 방법
### 방법 A. USB 케이블로 직접 복사
1. 휴대폰과 PC를 USB 케이블로 연결합니다.
2. 휴대폰에서 USB 사용 모드를 `파일 전송`으로 바꿉니다.
3. Windows 탐색기에서 갤럭시 저장소를 엽니다.
4. `Download` 폴더로 들어갑니다.
5. `app-debug.apk` 또는 `app-release-unsigned.apk`를 복사합니다.
6. 휴대폰에서 `내 파일` 앱을 열고 `Download` 폴더로 이동합니다.
7. APK 파일을 탭해 설치합니다.

### 방법 B. adb로 설치
PC에 Android SDK platform-tools가 있다면:
```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 방법 C. 카카오톡/이메일/드라이브로 옮기기
1. PC에서 APK 파일을 카카오톡 나에게 보내기, 이메일 첨부, Google Drive/OneDrive 업로드 중 하나로 전송합니다.
2. 갤럭시에서 해당 앱을 열고 APK 파일을 다운로드합니다.
3. 다운로드한 APK를 탭해 설치합니다.

## 11. 삼성 갤럭시에서 알 수 없는 앱 설치 허용 방법
One UI 버전에 따라 메뉴 이름이 조금 다를 수 있습니다.

일반적인 경로:
1. `설정`
2. `보안 및 개인정보 보호`
3. `기타 보안 설정`
4. `알 수 없는 앱 설치`
5. APK를 연 앱(내 파일, Chrome, Samsung Internet, 카카오톡 등)을 선택
6. `이 출처 허용` 켜기

다른 경로로 보일 수도 있습니다:
1. `설정`
2. `애플리케이션`
3. 우측 상단 더보기
4. `특별한 접근`
5. `알 수 없는 앱 설치`

설치 오류가 나면 APK를 연 앱에 대해 허용이 켜져 있는지 다시 확인하세요.

## 12. 오버레이 권한 켜는 방법
앱 내부에서 온보딩 또는 홈의 오버레이 버튼을 누르면 권한 화면으로 이동합니다.

수동 경로:
1. `설정`
2. `애플리케이션`
3. `특별한 접근`
4. `다른 앱 위에 표시`
5. `SOME` 선택
6. `허용` 켜기

## 13. 실제 사용 흐름
### 홈 화면에서 시작
1. 앱 실행
2. 상대 프로필 추가
3. `오버레이 시작` 버튼 누르기
4. 권한 허용
5. 채팅 앱으로 이동
6. 버블 탭
7. `현재 화면 분석` 누르기
8. 잠시 후 추천 답장 화면 확인

### 앱 내부 분석 시작 화면에서 시작
1. `새 분석 시작`
2. 상대 프로필 선택
3. `현재 화면 분석 시작`
4. 권한 허용 후 3초 안에 분석할 채팅 화면으로 이동
5. OCR 완료 후 추천 카드 확인

### 데모 모드
1. 분석 화면으로 이동
2. `데모 문장으로 미리 보기` 누르기
3. 실제 캡처 없이 추천 결과 확인

## 14. 버블이 안 보일 때 점검 방법
1. 오버레이 권한이 실제로 켜져 있는지 확인합니다.
2. 홈 화면에서 `오버레이 시작`을 눌렀는지 확인합니다.
3. 알림 영역에 `SOME 오버레이 실행 중` 알림이 있는지 확인합니다.
4. 삼성 갤럭시에서 배터리 최적화 때문에 서비스가 정지되지 않도록 점검합니다.

삼성 갤럭시 점검 경로 예시:
1. `설정`
2. `배터리 및 디바이스 케어`
3. `배터리`
4. `백그라운드 사용 제한`
5. `절전 앱` 또는 `자동으로 절전 앱에 추가`
6. SOME이 들어가 있으면 제외

추가로:
- 게임 부스터, 절전 모드, 자동 최적화 기능이 백그라운드 오버레이를 끄지 않는지 확인하세요.
- 아주 작은 화면 배율/글꼴 배율 환경에서는 버블 위치가 화면 가장자리에 붙어 보이지 않을 수 있으니 앱을 다시 열고 오버레이를 재시작하세요.

## 15. OCR이 잘 안 될 때 점검 방법
1. 채팅 화면이 전체 화면에 가깝게 보이도록 합니다.
2. 너무 빠르게 스크롤하는 순간이 아니라 멈춘 화면에서 분석합니다.
3. 상대 메시지와 내 메시지가 동시에 보이도록 최근 대화 몇 줄이 화면 안에 들어오게 합니다.
4. 글씨가 너무 작거나 대비가 낮으면 OCR 품질이 떨어질 수 있습니다.
5. 첫 설치 직후 ML Kit 초기 로딩 때문에 첫 분석이 약간 느릴 수 있습니다.

텍스트가 거의 없거나 OCR이 실패해도 앱은 죽지 않고 기본 추천 카드를 보여주도록 설계되어 있습니다.

## 16. 앱이 꺼질 때 점검 방법
1. Android 15 또는 One UI 최신 업데이트 후 권한이 초기화되지 않았는지 확인합니다.
2. 오버레이 권한을 껐다 켠 뒤 앱을 다시 실행합니다.
3. 설정 화면에서 기록을 지워 데이터 손상 가능성을 제거합니다.
4. 앱 정보를 열어 저장 공간에서 `캐시 삭제` 또는 `데이터 삭제`를 시도합니다.
5. debug APK를 다시 설치해 재현되는지 확인합니다.

## 17. 저장 데이터 초기화 방법
### 앱 내부에서 초기화
1. 앱 실행
2. `설정`
3. `전체 초기화`

### Android 시스템에서 초기화
1. `설정`
2. `애플리케이션`
3. `SOME`
4. `저장공간`
5. `데이터 삭제`

## 18. 빌드 체크리스트
- Android Studio에서 프로젝트가 열리는지 확인
- Gradle Sync 완료 확인
- `.\gradlew.bat assembleDebug` 성공 확인
- `.\gradlew.bat assembleRelease` 성공 확인
- debug APK 생성 경로 확인
- release unsigned APK 생성 경로 확인

## 19. 설치 체크리스트
- 갤럭시에서 `알 수 없는 앱 설치` 허용
- 필요 시 오버레이 권한 허용
- APK 파일을 휴대폰 저장소로 복사
- APK 탭 후 설치
- 앱 실행 후 온보딩 완료
- 상대 프로필 추가
- 오버레이 시작 후 채팅 화면에서 분석 테스트

## 20. 실제 빌드 확인 결과
이 작업 디렉터리에서 아래 두 빌드를 실제로 성공 확인했습니다.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

생성된 파일:
- debug: `app\build\outputs\apk\debug\app-debug.apk`
- release: `app\build\outputs\apk\release\app-release-unsigned.apk`
