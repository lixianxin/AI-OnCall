import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Search for the exact text right before the if(members.isEmpty())
idx = content.find("\u65e5\u7a0b\u53c2\u4e0e\u4eba")
if idx >= 0:
    start = idx
    end = min(len(content), idx + 600)
    print("FOUND:")
    print(content[start:end])
else:
    # Try without unicode escapes - search for raw text
    idx = content.find("members.chunked")
    if idx >= 0:
        start = max(0, idx - 200)
        end = min(len(content), idx + 200)
        print("FOUND via members.chunked:")
        print(content[start:end])
