# 피드백 API 명세

## 개요

기사 분석 결과에 대한 사용자 피드백을 저장합니다.

사용자는 분석 결과에 대해 좋아요 또는 싫어요를 선택하고, 선택적으로 코멘트를 남길 수 있습니다.

```text
1. 프론트가 기사 분석 결과 resultId를 가지고 있음
2. 사용자가 좋아요 또는 싫어요 선택
3. 사용자가 코멘트 입력 가능
4. 프론트가 피드백 저장 API 호출
5. 백엔드가 user_feedback 테이블에 저장
```

## Base URL

로컬 개발 기준:

```text
http://localhost:8080
```

## 1. 피드백 저장

기사 분석 결과에 대한 피드백을 저장합니다.

```http
POST /api/v1/feedback
```

### Request Headers

```http
Content-Type: application/json
```

### Request Body

좋아요 예시:

```json
{
  "resultId": 1,
  "feedbackType": "LIKE",
  "comment": "분석 결과가 도움이 됐어요."
}
```

싫어요 예시:

```json
{
  "resultId": 1,
  "feedbackType": "DISLIKE",
  "comment": "분석 결과가 조금 아쉬웠어요."
}
```

코멘트 없이도 저장할 수 있습니다.

```json
{
  "resultId": 1,
  "feedbackType": "LIKE",
  "comment": null
}
```

### Request Fields

| field | type | required | description |
| --- | --- | --- | --- |
| `resultId` | number | yes | 피드백을 남길 기사 분석 결과 ID입니다. `analysis_results.RESULT_ID` 값입니다. |
| `feedbackType` | string | yes | 피드백 타입입니다. `LIKE` 또는 `DISLIKE`만 가능합니다. |
| `comment` | string | no | 사용자가 남기는 코멘트입니다. 최대 1,000자입니다. |

### Success Response

```json
{
  "success": true,
  "data": {
    "id": 1,
    "resultId": 1,
    "feedbackType": "LIKE",
    "comment": "분석 결과가 도움이 됐어요."
  },
  "message": "피드백이 저장되었습니다."
}
```

### Response Fields

| field | type | description |
| --- | --- | --- |
| `id` | number | 저장된 피드백 ID입니다. |
| `resultId` | number | 피드백이 연결된 기사 분석 결과 ID입니다. |
| `feedbackType` | string | 저장된 피드백 타입입니다. `LIKE` 또는 `DISLIKE`입니다. |
| `comment` | string | 저장된 코멘트입니다. |

## Error Response

### 존재하지 않는 resultId

`resultId`에 해당하는 분석 결과가 없으면 실패합니다.

```json
{
  "success": false,
  "data": {
    "code": "ARTICLE_002",
    "message": "분석 결과가 아직 없습니다."
  },
  "message": "분석 결과가 아직 없습니다."
}
```

### 유효하지 않은 요청

`resultId` 또는 `feedbackType`이 없거나, `feedbackType` 값이 `LIKE`, `DISLIKE`가 아니면 실패합니다.

```json
{
  "success": false,
  "data": {
    "code": "COMMON_001",
    "message": "잘못된 요청입니다.",
    "errors": {
      "feedbackType": "좋아요 또는 싫어요를 선택해주세요."
    }
  },
  "message": "잘못된 요청입니다."
}
```
