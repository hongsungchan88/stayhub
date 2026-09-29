# 막혔을 때 먼저 볼 것

실행하다 에러가 나면 이 문서부터 찾아보세요. 여기 없거나 30분 넘게 안 풀리면
총괄에게 연락합니다. 연락할 때 ① 실행한 명령어 ② 에러 메시지 전문(스크린샷보다 텍스트)
③ `git status` 출력을 같이 보내주세요.

## 앱 실행

### `Failed to configure a DataSource: 'url' attribute is not specified`
`application-local.yml`이 없습니다.
`src/main/resources/application-local.yml.example`을 같은 폴더에 복사하고
이름을 `application-local.yml`로 바꾼 뒤 비밀번호를 본인 MySQL root 비밀번호로 채우세요.
비밀번호에 기호가 있으면 큰따옴표로 감쌉니다.

### `Unable to determine Dialect without JDBC metadata`
이 메시지 자체는 원인이 아닙니다. 로그를 **위로 올려서** `WARN` 줄을 찾으세요.

- `Unknown database 'stayhub'` (1049) — DB를 안 만든 겁니다. MySQL에서:
  `CREATE DATABASE stayhub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
- `Access denied for user 'root'` (1045) — 비밀번호가 틀렸습니다. `application-local.yml`을 확인하세요.

1049가 떴다면 비밀번호는 맞은 겁니다. 로그인은 통과하고 그다음 단계에서 막힌 것입니다.

### `Port 8080 was already in use`
이전에 실행한 앱이 안 꺼졌습니다. IntelliJ의 빨간 정지 버튼을 누르세요.
그래도 안 되면 명령 프롬프트에서:

```
netstat -ano | findstr :8080
taskkill /PID <맨 끝 숫자> /F
```

### 앱은 뜨는데 화면의 MySQL 버전이 8.4가 아님
PC에 MySQL 8.0과 8.4가 같이 깔려 있으면 먼저 켜진 쪽이 3306 포트를 잡습니다.
`services.msc` → `MySQL80` 중지, 시작 유형을 "수동"으로 → `MySQL84`가 실행 중인지 확인.
8.4가 3306이 아닌 포트로 잡혀 있으면 MySQL Configurator의 Reconfigure에서 3306으로 바꾸세요.
새로 붙은 8.4에는 `stayhub` DB를 다시 만들어야 합니다.

## 설치·설정

### `mysql --version`을 쳤더니 "명령을 찾을 수 없습니다"
설치가 잘못된 게 아니라 경로 등록이 안 된 것입니다.
시작 메뉴에서 "MySQL 8.4 Command Line Client"를 실행해 접속하면 됩니다.

### MySQL 설치 중 "Legacy Authentication Method" 선택지가 보임
고르지 마세요. 8.4는 옛날 방식(`mysql_native_password`)이 기본으로 꺼져 있고,
기본값 그대로 두는 게 정상입니다.

### IntelliJ가 Gradle이나 플러그인 업그레이드를 하라고 뜸
전부 거절하세요. 버전은 JDK 21 / Spring Boot 3.5.16 / Gradle 8.14.5 / MySQL 8.4로 고정입니다.
한 명만 수락해도 그 사람 빌드가 깨지고, 그게 커밋되면 전원이 깨집니다.

## Git

### push했더니 `GH013` / `Repository rule violations` 거절
main에 직접 push한 겁니다. main은 잠겨 있고, 이게 정상입니다.
아직 커밋 전이면 브랜치부터 만드세요:

```
git switch -c feature-{코드}-{작업명}
```

**이미 main에서 커밋해버렸다면** — 아래 명령으로 지금까지의 커밋을 그대로 새 브랜치로 옮긴 뒤 push하세요.

```
git switch -c feature-{코드}-{작업명}
git push -u origin feature-{코드}-{작업명}
```

그다음 GitHub에서 PR을 만듭니다. 로컬 main 정리는 총괄에게 물어보세요.

### push할 때 비밀번호를 묻는데 GitHub 비밀번호가 안 먹힘
GitHub는 비밀번호로 push하는 걸 막아두었습니다.
IntelliJ에서 Settings → Version Control → GitHub → `+` → **Log In via GitHub**로
계정을 연결하면 해결됩니다.
