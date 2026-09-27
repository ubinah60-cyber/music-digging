# DIGGER 홈 화면

React와 Vite로 만든 홈 화면입니다. Spring Boot의 `/`에서 빌드된 화면을 제공하며 기존 `/api` 코드는 변경하지 않습니다.

## Spring Boot에서 실행

```powershell
cd C:\dev\music-digging\frontend
npm.cmd ci
npm.cmd run build

cd C:\dev\music-digging
$env:LASTFM_API_KEY = '<기존 Last.fm API 키>'
.\gradlew.bat bootRun
```

MySQL은 기존 Spring Boot 설정에 맞게 실행되어 있어야 합니다. 브라우저에서 `http://127.0.0.1:8080/`을 엽니다. 이미 Spring Boot가 실행 중이었다면 새 빌드 파일과 페이지 경로가 반영되도록 재시작합니다. `npm.cmd run build`는 `src/main/resources/static/digger`를 갱신하므로 프런트엔드를 수정한 뒤 다시 실행합니다. 이 빌드 결과가 JAR에 포함되어 배포 서버에는 Node가 필요하지 않습니다.

`npm.cmd`를 찾을 수 없다면 이 PC의 IntelliJ에 포함된 Node 디렉터리를 **현재 PowerShell 창**의 PATH에 추가합니다.

```powershell
$nodeDir = Join-Path $env:LOCALAPPDATA 'JetBrains\IntelliJIdea2026.2\acp-agents\.runtimes\node\24.19.0'
$env:Path = "$nodeDir;$env:Path"
npm.cmd --version
```

새 PowerShell 창을 열면 PATH 설정을 다시 해야 합니다. `npm`의 PowerShell 스크립트 실행이 차단되는 경우에도 `npm.cmd`를 사용합니다.

## 프런트엔드 개발 서버

화면 작업 중에는 `frontend`에서 `npm.cmd run dev`를 실행하고 `http://127.0.0.1:5173/`을 엽니다. 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 전달합니다. 화면 자체는 백엔드 없이도 열립니다.

검색창을 선택하면 검색 화면으로 전환됩니다. 검색창 바깥의 배경을 클릭하거나 왼쪽 위 `DIGGER`, 하단 `메인`, 또는 `Esc` 키를 누르면 홈으로 돌아옵니다. 배경의 연결선과 빛은 천천히 움직이며, 운영체제에서 동작 줄이기를 선택한 경우 애니메이션을 멈춥니다.
