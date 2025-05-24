curl http://47.122.114.115:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
        "model": "deepseek-r1:1.5b",
        "prompt": "你是谁",
        "stream": false
      }'