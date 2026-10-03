# FrameLESS

뉴스 기사 하나를 분석하고 같은 사건의 다른 보도와 문장 표현을 비교하는 프로젝트입니다.
현재 Android 화면은 예시 데이터를 보여주며, 이 브랜치는 기존 Python 분석 함수를
로컬 Django API로 호출할 수 있게 합니다.

## 로컬 백엔드 실행

Python 3.14에서 확인했습니다. Gemini와 NAVER API HUB 키는 서버 프로세스에만
설정하고 Git이나 Android 앱에 넣지 마세요. `NAVER_CLIENT_ID`와
`NAVER_CLIENT_SECRET`은 일반 NAVER Developers Open API가 아닌
[NAVER API HUB](https://api.ncloud-docs.com/docs/naver-api-hub-search-news)의 값입니다.

```sh
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# .env 파일에 세 키의 실제 값을 입력한 뒤:
chmod 600 .env
set -a
source .env
set +a
python manage.py runserver
```

서버는 기본적으로 이 컴퓨터의 `127.0.0.1:8000`에서 실행됩니다. 다른 터미널에서
상태 확인과 분석 요청을 보낼 수 있습니다.

```sh
curl http://127.0.0.1:8000/api/health
curl -X POST http://127.0.0.1:8000/api/analyze \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://n.news.naver.com/article/057/0001971678","max_related":3}'
```

`/api/health`의 `analysis_ready`가 `true`면 세 가지 키가 모두 설정된 상태입니다.
키가 빠졌다면 `missing_configuration`에 해당 환경변수 이름이 표시됩니다.

`max_related`는 생략할 수 있으며 1~3 사이의 정수입니다. 성공 응답은 기존 분석
함수의 결과를 그대로 JSON으로 반환합니다. 주요 필드는 `source_analysis`,
`related_articles`, `candidates`, `screened`, `comparison`입니다. 오류는
`{"error":{"code":"...","message":"..."}}` 형태입니다. 분석 중에는 다른
요청에 HTTP 429와 `Retry-After` 헤더를 반환합니다. 여러 기사와 Gemini 응답을
기다려야 하므로 한 번의 분석에 시간이 걸릴 수 있습니다.

키가 없어도 서버 시작, `/api/health`, 입력 검증 테스트는 실행할 수 있습니다.
실제 비교 결과를 받으려면 위 두 서비스의 유효한 키가 필요합니다.

```sh
python manage.py check
python manage.py test
```

현재 분석 결과를 저장하지 않아 데이터베이스는 사용하지 않습니다. 기사 URL은
HTTP(S) 도메인 주소를 받으며 localhost, IP 주소, 별도 포트는 거부합니다.
이 서버는 **로컬 개발용**입니다. 인증이 없으므로 인터넷에 공개하지 마세요.

Android 에뮬레이터에서 나중에 API를 연결할 때 서버 주소는
`http://10.0.2.2:8000`입니다. 현재 Android 앱에는 네트워크 호출이 없고
예시 데이터만 표시됩니다. 실제 연결 시 인터넷 권한과 개발용 HTTP 설정도
추가해야 합니다.
