import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Need to add a closing } for the outer forEach after the Row's closing }
# Current: "            }\n        }\n    }\n}"
# Should be: "            }\n            }\n        }\n    }\n}"
old = """                }
            }
        }
    }
}"""

new = """                }
            }
            }
        }
    }
}"""

# Only replace the specific one near ParticipantSelector
# Find the text after "onToggle(member.userId)"
idx = content.find("onToggle(member.userId)")
if idx >= 0:
    # Search for the pattern after this point
    search_start = idx
    # Find the closing pattern
    old_idx = content.find(old, search_start)
    if old_idx >= 0 and old_idx < search_start + 200:
        content = content[:old_idx] + new + content[old_idx + len(old):]
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        print("Fixed: added missing } for forEach")
    else:
        print(f"Pattern not found near index {search_start}")
else:
    print("onToggle not found")
