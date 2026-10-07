import os

# Fix OpenOnCallRepository.kt - parse SourcesEvent properly
filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\repository\OpenOnCallRepository.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Fix the Sources parsing - parse JSON objects, not strings
old_sources = '''                "sources" -> {
                    val arr = obj.optJSONArray("sources")
                    val list = if (arr != null) (0 until arr.length()).map { i -> arr.optString(i, "") } else emptyList()
                    OnCallStreamEvent.Sources(items = list.map { OnCallStreamEvent.SourceItem(file = it, relevance = 0.0, snippet = "") })
                }'''

new_sources = '''                "sources" -> {
                    val arr = obj.optJSONArray("sources")
                    val list = if (arr != null) {
                        (0 until arr.length()).mapNotNull { i ->
                            val item = arr.optJSONObject(i) ?: return@mapNotNull null
                            OnCallStreamEvent.SourceItem(
                                file = item.optString("file", ""),
                                relevance = item.optDouble("relevance", 0.0),
                                snippet = item.optString("snippet", "")
                            )
                        }
                    } else emptyList()
                    OnCallStreamEvent.Sources(items = list)
                }'''

if old_sources in content:
    content = content.replace(old_sources, new_sources)
    print("SourcesEvent parsing fixed")
else:
    print("WARNING: old sources pattern not found")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Repository done")
