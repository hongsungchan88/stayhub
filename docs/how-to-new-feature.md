# 새 기능 만드는 법

숙소 가이드북(CMS-002)이 **참고 구현**입니다. 테이블 하나에 등록·조회·수정·삭제를 만드는 기능은
전부 가이드북과 같은 모양으로 만듭니다. 새로운 방식을 고민하지 말고, 가이드북을 베끼세요.

모양이 모두 같아야 서로의 코드를 바로 읽을 수 있고, 리뷰할 때 "가이드북과 다른 곳"만 보면 됩니다.

## 순서

### ① main 최신으로 받고 브랜치 만들기

```
git switch main
git pull
git switch -c feature-{코드}-{작업명}
```

예) 객실 기능이면 `feature-rom-room`. 코드는 README "## 담당" 표를 보세요.

### ② 가이드북 파일 열어두기

아래 파일을 IntelliJ에서 열어두세요. 경로는 모두 `src/main/java/com/stayhub/` 아래입니다.

| 파일 | 하는 일 | 복사해서 내 것으로 |
| --- | --- | --- |
| `content/entity/Guidebook.java` | 테이블 한 행 | O |
| `content/entity/GuidebookCategory.java` | enum 컬럼의 값 목록 | enum 컬럼이 있을 때만 |
| `content/repository/GuidebookRepository.java` | DB 조회 | O |
| `content/service/GuidebookService.java` | 업무 로직 | O |
| `content/controller/GuidebookController.java` | API 주소 | O |
| `content/dto/GuidebookCreateRequest.java` | 등록 요청 | O |
| `content/dto/GuidebookUpdateRequest.java` | 수정 요청 | O |
| `content/dto/GuidebookResponse.java` | 응답 | O |
| `src/test/java/com/stayhub/content/GuidebookServiceTest.java` | 테스트 | O |

**`common/` 폴더 파일은 복사하지 마세요.** `ApiResponse`, `BusinessException`, `BaseEntity`,
`EntityStatus`, `PageResponse`, `PropertyScope` 는 모든 기능이 같이 쓰는 파일이라 그대로 import 합니다.

각 파일에서 바꿔야 하는 줄에는 `// ★ 바꿀 곳:` 주석이 붙어 있습니다.
IntelliJ에서 `Ctrl + Shift + F` → `★ 바꿀 곳` 으로 검색하면 한 번에 모아볼 수 있습니다.

가이드북에만 있는 규칙이 두 가지 있습니다. 내 테이블에 없으면 지우세요.
- **중복 제목 경고** — 같은 카테고리에 같은 제목이 있으면 저장은 하되 경고 메시지를 붙입니다
- **카테고리 필터** — 목록을 카테고리로 거릅니다

### ③ AI에게 요청하기

아래 틀을 그대로 복사해서 `[ ]` 부분만 채워 넣으세요.

```
content 도메인의 Guidebook 관련 파일들을 참고해서, 똑같은 구조로
[테이블명] 기능을 [도메인 폴더]에 만들어줘.
테이블 정의: [테이블 정의서 행 붙여넣기]
- 다른 테이블을 가리키는 컬럼은 Long ID 값으로만 (@ManyToOne 금지)
- 컬럼·테이블 이름은 테이블 정의서와 한 글자도 다르지 않게
- 가이드북에 없는 새로운 방식은 쓰지 말 것
```

예) `[테이블명]` → `rooms`, `[도메인 폴더]` → `room`,
`[테이블 정의서 행 붙여넣기]` → 테이블 정의서의 rooms 테이블 행을 통째로 복사

### ④ 가이드북과 나란히 비교하기

AI가 만든 파일과 가이드북 파일을 하나씩 나란히 놓고 봅니다.
IntelliJ에서 두 파일을 `Ctrl` 을 누른 채 함께 선택 → `Ctrl + D` 를 누르면 좌우로 비교됩니다.

이런 게 보이면 고쳐야 합니다.
- 가이드북에 없는 어노테이션 — `@Setter`, `@Data`, `@Builder`, `@ManyToOne`, `@Lob` 등
- `// ★ 바꿀 곳:` 줄인데 가이드북 이름이 그대로 남아 있음 — `Guidebook`, `guidebook`, `가이드북` 으로 검색
- 컬럼명·테이블명이 테이블 정의서와 한 글자라도 다름
- enum 컬럼에 `columnDefinition = "VARCHAR(n)"` 이 빠짐

### ⑤ 실행해서 확인하기

1. **테스트** — IntelliJ에서 테스트 클래스 이름 왼쪽 초록 화살표, 또는 터미널에서 `./gradlew test`.
   MySQL이 켜져 있어야 합니다.
2. **테이블 확인** — 앱을 실행하면 테이블이 자동으로 만들어집니다. MySQL에서 아래를 실행해
   테이블 정의서와 컬럼 이름·타입을 한 줄씩 대조하세요. 특히 enum 컬럼이 `varchar` 인지 확인합니다.
   ```
   SHOW CREATE TABLE 내테이블명;
   ```
3. **API 호출** — 목록·상세는 브라우저 주소창에 바로 넣어보면 됩니다.
   예) <http://localhost:8080/api/guidebooks>
   등록·수정·삭제는 Git Bash에서 curl로 확인합니다.
   ```
   curl -X POST http://localhost:8080/api/guidebooks -H "Content-Type: application/json" -d '{"category":"WIFI","title":"와이파이","content":"비밀번호 1234"}'
   curl -X PUT http://localhost:8080/api/guidebooks/1 -H "Content-Type: application/json" -d '{"category":"WIFI","title":"와이파이 안내","content":"비밀번호 5678"}'
   curl -X DELETE http://localhost:8080/api/guidebooks/1
   ```
   응답의 `"success":true` 를 확인하세요. 삭제한 뒤 같은 ID로 상세 조회하면 `"success":false` 가 나와야 정상입니다.

막히면 [troubleshooting.md](troubleshooting.md)를 먼저 보세요.

### ⑥ PR 올리기

1. 커밋 메시지는 `[도메인] 작업내용` — 예) `[ROM] 객실 CRUD 추가`
2. push 한 뒤 GitHub에서 main 으로 향하는 PR을 만듭니다
3. PR 본문에 **무엇을 / 왜 / 확인한 것 / 관련 기능ID** 네 항목을 씁니다.
   "확인한 것"에는 ⑤에서 해본 것(테스트 통과, `SHOW CREATE TABLE` 대조, API 호출 결과)을 적으세요
4. 변경 파일은 20개 이하

## 규약 체크리스트

PR을 올리기 전에 하나씩 체크하세요. 가이드북은 전부 지키고 있습니다.

- [ ] **1. `@Table(name = "복수형")` 명시, `@Column(name = "...")` 으로 컬럼명 전부 명시**
  — 빼면 테이블명이 클래스명(단수형)으로 만들어져 정의서와 달라지고, 적어두면 정의서와 한 줄씩 대조할 수 있습니다.
- [ ] **2. enum 은 `@Enumerated(EnumType.STRING)` + `columnDefinition = "VARCHAR(n)"`**
  — 기본값(ORDINAL)은 0·1·2 순번으로 저장돼 값을 중간에 추가하면 기존 데이터 뜻이 바뀌고, VARCHAR 를 빼면 MySQL ENUM 타입이 되어 값을 추가할 때 저장이 실패합니다.
- [ ] **3. 다른 테이블은 `@ManyToOne` 대신 `Long xxxId` 값으로만. `property_id` 는 등록할 때 서비스에서 `PropertyScope.DEFAULT_PROPERTY_ID` 를 넣고, 요청에서 받지 않으며, 조회 조건에는 넣지 않는다 (property_id 가 있는 5개 테이블만 해당 — rooms, properties, property_images, property_facilities, guidebooks)**
  — `open-in-view: false` 라서 연관관계는 서비스 밖에서 지연 로딩 예외가 나고, 1차는 숙소가 하나라 조회에 숙소 조건을 걸지 않되 컬럼은 정의해 둡니다(정책정의서 32행).
- [ ] **4. Lombok 은 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 만. 생성은 `create(...)`, 변경은 `update(...)`·`delete()`**
  — `@Setter` 가 있으면 어디서 값이 바뀌었는지 추적이 안 되고 `@Builder` 는 필수값을 빠뜨려도 컴파일되니, 값이 바뀌는 곳을 세 메서드로 모아둡니다.
- [ ] **5. 컨트롤러는 엔티티를 반환하지 않는다. 응답 DTO 변환은 서비스 안에서**
  — 엔티티를 그대로 내보내면 내부 값이 새어 나가고 컬럼을 추가할 때마다 API 응답이 같이 바뀌어 화면이 깨집니다.
- [ ] **6. 서비스 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`**
  — 조회 메서드에서 실수로 값을 바꿔도 DB에 반영되지 않고, 어떤 메서드가 DB를 바꾸는지 한눈에 보입니다.
- [ ] **7. 응답은 전부 `ApiResponse` 로 감싼다**
  — 성공이든 실패든 응답 모양이 하나라서 화면 쪽은 한 가지 방식으로만 처리하면 됩니다.
- [ ] **8. 없거나 DELETED 인 항목은 `throw new BusinessException("○○을 찾을 수 없습니다.")`**
  — 삭제된 항목은 사용자에게 없는 것과 같고, 이 메시지는 그대로 화면에 표시되니 사용자가 읽을 문장으로 씁니다.
