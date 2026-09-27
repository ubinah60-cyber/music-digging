# DIGGER 홈 화면

React와 Vite로 만든 홈 화면입니다. 기존 Spring Boot API 코드는 변경하지 않습니다.

## 실행

```powershell
cd C:\dev\music-digging\frontend
npm.cmd ci
npm.cmd run dev
```

`npm.cmd`를 찾을 수 없으면 이 PC의 IntelliJ에 포함된 Node 디렉터리를 **현재 PowerShell 창**의 PATH에 추가한 다음 다시 실행합니다.

```powershell
$nodeDir = Join-Path $env:LOCALAPPDATA 'JetBrains\IntelliJIdea2026.2\acp-agents\.runtimes\node\24.19.0'
$env:Path = "$nodeDir;$env:Path"
npm.cmd --version
npm.cmd ci
npm.cmd run dev
```

브라우저에서 `http://127.0.0.1:5173`을 엽니다. 새 PowerShell 창을 열면 위 PATH 설정을 다시 해야 합니다. `npm`의 PowerShell 스크립트 실행이 차단되는 경우에도 `npm.cmd`를 사용합니다.

검색창을 선택하면 검색 화면으로 전환됩니다. 검색창 바깥의 배경을 클릭하거나 왼쪽 위 `DIGGER`, 하단 `메인`, 또는 `Esc` 키를 누르면 홈으로 돌아옵니다. 배경의 연결선과 빛은 천천히 움직이며, 운영체제에서 동작 줄이기를 선택한 경우 애니메이션을 멈춥니다.

검색을 사용하려면 기존 설정에 맞는 MySQL과 Spring Boot를 실행한 뒤 `http://localhost:8080`에 접근할 수 있어야 합니다. 백엔드 실행에는 기존 설정의 `LASTFM_API_KEY` 환경 변수도 필요합니다. 개발 서버는 `/api` 요청을 그 주소로 전달합니다. 홈 화면 자체는 백엔드 없이도 열립니다.

```powershell
cd C:\dev\music-digging
$env:LASTFM_API_KEY = '<기존 Last.fm API 키>'
.\gradlew.bat bootRun
```

배포용 프런트엔드 파일은 `npm.cmd run build`로 `frontend/dist`에 생성합니다. 현재 Spring Boot의 `/` 페이지는 기존 Thymeleaf 화면이므로 React 화면은 별도 주소 `:5173`에서 확인합니다.
