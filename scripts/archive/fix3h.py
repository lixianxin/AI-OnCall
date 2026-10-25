import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Find all "if (members.isEmpty())" and pick the one nearest to 195783
pos = 0
occurrences = []
while True:
    idx = content.find("if (members.isEmpty())", pos)
    if idx < 0:
        break
    occurrences.append(idx)
    pos = idx + 1

# Find the one closest to 195783
closest = min(occurrences, key=lambda x: abs(x - 195783))
print(f"Closest if(members.isEmpty()) at {closest}")
print(f"Distance: {abs(closest - 195783)}")

# Show context
start = max(0, closest - 200)
end = min(len(content), 196500)
print(content[start:end])
