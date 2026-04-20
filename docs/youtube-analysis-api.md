# 유튜브 분석 API 명세서

## 개요

유튜브 분석은 비동기 방식으로 동작합니다.

프론트는 먼저 유튜브 URL로 분석 요청을 접수하고, 응답으로 받은 `requestId`를 사용해 분석 결과를 주기적으로 조회합니다.

```text
1. 프론트가 유튜브 URL로 분석 요청
2. 백엔드가 videoId 추출 후 분석 요청 접수
3. 백엔드가 requestId 반환
4. 백엔드가 비동기로 AI 서버 분석 요청
5. 프론트가 requestId로 결과 polling
6. 분석 완료 시 result 표시
```

## Base URL

로컬 개발 기준:

```text
http://localhost:8080
```

## 상태값

| status | 설명 |
| --- | --- |
| `PENDING` | 분석 요청이 접수된 상태 |
| `PROCESSING` | AI 분석이 진행 중인 상태 |
| `COMPLETED` | 분석이 완료된 상태 |
| `FAILED` | 분석이 실패한 상태 |

## 1. 분석 요청 접수

유튜브 URL을 전달하면 분석 요청을 접수하고, 결과 조회에 사용할 `requestId`를 반환합니다.

```http
POST /api/youtube/analysis
```

### Request Headers

```http
Content-Type: application/json
```

### Request Body

```json
{
  "youtubeUrl": "https://www.youtube.com/watch?v=Avt_ySKe3fE"
}
```

### Success Response

```json
{
  "success": true,
  "data": {
    "requestId": "50afbb15-0788-477c-807b-fcd09b350116",
    "youtubeId": "Avt_ySKe3fE",
    "status": "PENDING"
  },
  "message": "분석 요청이 접수되었습니다."
}
```

### URL 오류 응답 예시

유튜브 URL이 아니거나 videoId를 추출할 수 없는 경우입니다.

```json
{
  "success": false,
  "data": {
    "code": "YOUTUBE_001",
    "message": "유효하지 않은 유튜브 URL입니다."
  },
  "message": "유효하지 않은 유튜브 URL입니다."
}
```

지원하지 않는 유튜브 URL 형식인 경우입니다.

```json
{
  "success": false,
  "data": {
    "code": "YOUTUBE_002",
    "message": "지원하지 않는 유튜브 URL 형식입니다."
  },
  "message": "지원하지 않는 유튜브 URL 형식입니다."
}
```

## 2. 분석 결과 조회

분석 요청 접수 시 받은 `requestId`로 현재 분석 상태와 결과를 조회합니다.

프론트는 이 API를 2~3초 간격으로 polling 하면 됩니다.

```http
GET /api/youtube/analysis/{requestId}
```

### Request Example

```http
GET /api/youtube/analysis/50afbb15-0788-477c-807b-fcd09b350116
```

### Processing Response

분석이 아직 진행 중인 경우입니다.

```json
{
  "success": true,
  "data": {
    "requestId": "50afbb15-0788-477c-807b-fcd09b350116",
    "youtubeId": "Avt_ySKe3fE",
    "status": "PROCESSING",
    "result": null,
    "errorMessage": null
  },
  "message": "분석 요청 조회 성공"
}
```

### Completed Response

분석이 완료된 경우입니다.

```json
{
  "success": true,
  "data": {
    "requestId": "50afbb15-0788-477c-807b-fcd09b350116",
    "youtubeId": "Avt_ySKe3fE",
    "status": "COMPLETED",
    "result": {
      "videoTitle": "영상 제목",
      "videoCommentCount": "12345",
      "total": 98,
      "positive": 40,
      "negative": 20,
      "neutral": 38,
      "positivePct": 40.8,
      "negativePct": 20.4,
      "neutralPct": 38.8,
      "botCount": 5,
      "botPct": 5.1,
      "summary": "전반적 여론 요약",
      "comments": [
        {
          "text": "댓글 내용",
          "likes": 123,
          "sentiment": "긍정",
          "sentimentScore": 0.87,
          "botScore": 10,
          "isBot": false,
          "botReasons": []
        },
        {
          "text": "반드시 촉구해야 합니다. 올바른 방향으로",
          "likes": 0,
          "sentiment": "중립",
          "sentimentScore": 0.71,
          "botScore": 75,
          "isBot": true,
          "botReasons": [
            "선동 키워드",
            "딱딱한 문체"
          ]
        }
      ]
    },
    "errorMessage": null
  },
  "message": "분석 요청 조회 성공"
}
```

### Failed Response

분석 요청은 접수되었지만 AI 서버 요청 또는 분석 처리 중 문제가 발생한 경우입니다.

```json
{
  "success": true,
  "data": {
    "requestId": "50afbb15-0788-477c-807b-fcd09b350116",
    "youtubeId": "Avt_ySKe3fE",
    "status": "FAILED",
    "result": null,
    "errorMessage": "AI 서버에 분석 요청을 보내지 못했습니다. 잠시 후 다시 시도해주세요."
  },
  "message": "분석 요청 조회 성공"
}
```

### Not Found Response

존재하지 않는 `requestId`로 조회한 경우입니다.

```json
{
  "success": false,
  "data": {
    "code": "ANALYSIS_001",
    "message": "분석 요청을 찾을 수 없습니다."
  },
  "message": "분석 요청을 찾을 수 없습니다."
}
```

## 프론트 처리 흐름

1. 사용자가 유튜브 URL 입력
2. `POST /api/youtube/analysis` 호출
3. 응답의 `data.requestId` 저장
4. `GET /api/youtube/analysis/{requestId}`를 2~3초 간격으로 호출
5. `status`가 `PENDING` 또는 `PROCESSING`이면 로딩 표시
6. `status`가 `COMPLETED`이면 `data.result`를 화면에 표시
7. `status`가 `FAILED`이면 `data.errorMessage`를 화면에 표시

## 참고

백엔드는 AI 서버에 다음 형식으로 요청합니다.

```http
GET {YOUTUBE_AI_ANALYSIS_URL}/{videoId}
```

현재 기본 설정값:

```text
http://localhost:8000/analyze/youtube
```

예시:

```http
GET http://localhost:8000/analyze/youtube/Avt_ySKe3fE
```
