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
  "youtubeUrl": "https://www.youtube.com/watch?v=jjdmX3GPdeQ"
}
```

### Success Response

```json
{
  "success": true,
  "data": {
    "requestId": "50afbb15-0788-477c-807b-fcd09b350116",
    "youtubeId": "jjdmX3GPdeQ",
    "status": "PENDING"
  },
  "message": "분석 요청이 접수되었습니다."
}
```

### URL 오류 응답 예시

유효하지 않은 유튜브 URL이거나 `videoId`를 추출할 수 없는 경우입니다.

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
    "youtubeId": "jjdmX3GPdeQ",
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
    "requestId": "f2f4473c-d6d6-48a9-91c7-3de0d5bf735c",
    "youtubeId": "jjdmX3GPdeQ",
    "status": "COMPLETED",
    "result": {
      "videoTitle": "체스오프닝 12분 입문가이드 [킹즈갬빗 King’s Gambit] 용기있는 자들을 위한 양날의 검",
      "channelName": "체스프릭김창훈",
      "viewCount": 29804,
      "publishedAt": "2024-10-20",
      "videoCommentCount": "46",
      "total": 41,
      "positive": 32,
      "negative": 0,
      "neutral": 9,
      "positivePct": 78,
      "negativePct": 0,
      "neutralPct": 22,
      "botCount": 0,
      "botPct": 0,
      "positiveSummary": "긍정 댓글들은 킹즈갬빗과 관련된 매력과 짜릿함을 강조하고 있습니다.",
      "negativeSummary": "부정 댓글 없음",
      "neutralSummary": "중립 댓글들은 주로 영상의 내용에 대한 피드백을 제공하고 있습니다.",
      "specialNotes": "없음",
      "comments": [
        {
          "text": "영상에 오타가 있습니다😅",
          "likes": 15,
          "authorName": "@chessfreak",
          "authorId": "UCO5rDIUWfCX7gsCzURXMUCg",
          "sentiment": "중립",
          "sentimentScore": 0.85,
          "botScore": 0,
          "isBot": false,
          "botReasons": []
        },
        {
          "text": "서로 킹을 갬빗해서 킹즈갬빗인거군요!",
          "likes": 33,
          "authorName": "@spicca4601",
          "authorId": "UC_gKDl1llqCPbENKMvQL1ZQ",
          "sentiment": "긍정",
          "sentimentScore": 0.9,
          "botScore": 0,
          "isBot": false,
          "botReasons": []
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
    "youtubeId": "jjdmX3GPdeQ",
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

로컬 개발 예시:

```text
http://localhost:8000/analyze/youtube
```

예시:

```http
GET http://localhost:8000/analyze/youtube/jjdmX3GPdeQ
```
