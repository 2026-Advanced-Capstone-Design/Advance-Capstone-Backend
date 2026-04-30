from flask import Flask
from flask_cors import CORS
from routes.analyze import analyze_bp
from routes.status import status_bp

app = Flask(__name__)
CORS(app)

#블루 프린트 객체를 등록하는 과정
#분석 관련 기능을 담은 블루 프린트 객체를 등록하는 과정

app.register_blueprint(analyze_bp)
app.register_blueprint(status_bp)


@app.get("/health")
def health():
    return {"status": "ok"}, 200

# 모든 IP 접근 허용, 차후에 막는것도...?
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
