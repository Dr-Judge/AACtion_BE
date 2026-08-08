# 로컬 개발 환경 세팅

클론 후 **한 번만** 하면 된다. 순서대로 하면 애플리케이션이 뜬다.

> 이 문서가 없어서 실제로 막힌 사례가 있다(#44). 순서 중 하나라도 빠지면
> **원인을 가리키지 않는 에러**가 난다 — 증상별 원인은 맨 아래 표에 있다.

---

## 필요한 것

- JDK 17
- MySQL 8 (로컬 실행)

---

## 1. MySQL 계정·데이터베이스 만들기

MySQL을 새로 설치하면 `root`만 있다. 애플리케이션이 쓰는 계정을 만든다:

```sql
CREATE USER IF NOT EXISTS 'drjudge'@'localhost' IDENTIFIED BY '본인이_정할_비밀번호';
-- ⚠️ CREATE USER IF NOT EXISTS 는 계정이 이미 있으면 비밀번호를 바꾸지 않는다(조용히 넘어간다).
--    그래서 ALTER USER 를 함께 실행한다 — 신규·기존 계정 모두 같은 비밀번호가 된다.
--    이게 없으면 "계정은 만들었는데 Access denied"가 계속 난다.
ALTER USER 'drjudge'@'localhost' IDENTIFIED BY '본인이_정할_비밀번호';
CREATE DATABASE IF NOT EXISTS drjudge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON drjudge.* TO 'drjudge'@'localhost';
FLUSH PRIVILEGES;
```

> 위 두 곳의 비밀번호는 같은 값이어야 하고, 2번에서 `application-secret.yml`의 `DB_PASSWORD`에도 같은 값을 넣는다.

> 테이블은 만들지 않는다. **스키마의 주인은 Flyway**이고, 애플리케이션이 뜰 때 마이그레이션이 적용된다.
> 상세: [DB_MIGRATION_RULES.md](DB_MIGRATION_RULES.md)

## 2. 설정 파일 만들기

```bash
cp application-secret.yml.example application-secret.yml
```

`DB_PASSWORD`에 1번 비밀번호, `JWT_SECRET`은 로컬에서 아무 값이나(생성: `openssl rand -hex 32`) 넣는다.

프로젝트 **루트**에 둔다. `src/main/resources` 아래에 두면 읽히지 않고, `Dockerfile`의 `COPY src` 때문에 **이미지에 비밀값이 실려 나간다.**

> `.gitignore` 처리돼 있다. 커밋되지 않는다.

## 3. 애플리케이션 실행

IntelliJ에서 `DrJudgeApplication`을 실행하거나:

```bash
./gradlew bootRun
```

성공하면 이렇게 보인다:

```text
Migrating schema `drjudge` to version "1 - init schema"
...
Successfully applied N migrations to schema `drjudge`
Tomcat started on port 8080 (http)
Started BackendApplication in N seconds
```

확인:

```bash
curl http://localhost:8080/actuator/health     # {"status":"UP"}
```

---

## 증상별 원인

`spring.config.import`가 `optional:`이라 **설정 파일이 없거나 키가 빠져도 경고가 없다.** 그래서 실패가 엉뚱한 곳에서 터진다. 아래 표로 역추적한다.

| 증상 | 원인 | 조치 |
|---|---|---|
| `Could not resolve placeholder 'DB_HOST'` | `application-secret.yml`이 없거나 루트에 없음 | 2번. `src/main/resources`가 아니라 **루트** |
| `UnknownHostException (${DB_HOST})` | 같은 원인 — 치환 안 된 문자열이 그대로 접속 시도 | 2번 |
| `Could not resolve placeholder 'JWT_SECRET'` | secret 파일에 그 키가 없음(기본값 없는 키) | 2번 |
| `Access denied for user 'drjudge'@'localhost'` | MySQL 계정이 없거나 `DB_PASSWORD`가 틀림 | 1번 |
| `Communications link failure` | MySQL이 안 떠 있음 / `DB_HOST` 오타 | MySQL 서비스 확인 |
| `Schema validation: wrong column type ...` | 엔티티와 마이그레이션 불일치 | **코드 버그다.** 이슈로 올릴 것(#44 참고) |
| `Port 8080 was already in use` | 이전 인스턴스가 살아 있음 | 그 프로세스 종료 |

> 마지막 항목 주의: `ddl-auto: validate`라서 엔티티와 실제 스키마가 다르면 부팅이 막힌다. **내 설정 문제가 아니라 코드 문제**다.
> 테스트는 H2 + `create-drop`이라 이 불일치를 잡지 못한다(스키마를 엔티티에서 생성하므로) — 항상 로컬에서 먼저 발견된다.
