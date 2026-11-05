import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.request import Request, urlopen

ORIGIN = "https:" + "/" + "/" + "mhndbsyky25-cmd.github.io"

class Handler(BaseHTTPRequestHandler):
    def respond(self, status, data):
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", ORIGIN)
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "POST, OPTIONS")
        self.end_headers()
        self.wfile.write(body)

def do_OPTIONS(self):
        self.respond(200, {})

def do_GET(self):
        self.respond(200, {"status": "ok"})

def do_POST(self):
        if self.path != "/chat":
            self.respond(404, {"error": "not found"})
            return

key = os.environ.get("OPENROUTER_API_KEY")
        if not key:
            self.respond(503, {"error": "service not configured"})
            return

try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > 30000:
                raise ValueError("invalid length")

data = json.loads(self.rfile.read(length))
            incoming = data["messages"]
            if not isinstance(incoming, list) or not incoming:
                raise ValueError("invalid messages")

messages = []
            for item in incoming[-10:]:
                if not isinstance(item, dict):
                    raise ValueError("invalid message")
                role = item.get("role")
                content = item.get("content")
                if role not in ("user", "assistant"):
                    raise ValueError("invalid role")
                if not isinstance(content, str) or len(content) > 4000:
                    raise ValueError("invalid content")
                messages.append({"role": role, "content": content})

payload = json.dumps({
                "model": "deepseek/deepseek-r1-0528:free",
                "messages": messages,
                "max_tokens": 500
            }).encode("utf-8")

url = "https:" + "/" + "/" + "openrouter.ai/api/v1/chat/completions"
            request = Request(
                url,
                data=payload,
                headers={
                    "Authorization": "Bearer " + key,
                    "Content-Type": "application/json"
                },
                method="POST"
            )
            with urlopen(request, timeout=90) as response:
                result = json.load(response)

reply = result["choices"][0]["message"]["content"]
            if not isinstance(reply, str) or not reply.strip():
                raise ValueError("empty reply")
            self.respond(200, {"reply": reply})
        except Exception:
            self.respond(502, {"error": "request failed"})

port = int(os.environ.get("PORT", "10000"))
ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()
