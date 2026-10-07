import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: } else {members -> } else {\n            members
content = content.replace("} else {members.chunked", "} else {\n            members.chunked")

# Fix 2: Remove extra closing braces (2 lines after the forEach block closes)
# The pattern: "        } \n            }\n        }\n    }\n}"
# Should be: "        }\n    }\n}"
old = """        } 
            }
        }
    }
}"""
new = """        }
    }
}"""

if old in content:
    content = content.replace(old, new)
    print("Fixed extra braces")
else:
    print("Extra braces pattern not found")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Done cleaning up")
