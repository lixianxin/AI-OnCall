import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Check occurrence 10 at 196412
idx = 196412
start = max(0, idx - 80)
end = min(len(content), idx + 100)
print(f"Context around 196412:")
print(content[start:end])
print("---")

# Also check the area between members.chunked at 195783 and this centerTitle at 196412
print(f"Between 195783 and 196412 ({196412-195783} chars):")
print(content[195783:196500])
