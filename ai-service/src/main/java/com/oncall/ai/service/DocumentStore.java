package com.oncall.ai.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DocumentStore {

    private static final Logger log = LoggerFactory.getLogger(DocumentStore.class);

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;
    private static final double BM25_K1 = 1.5;
        private static final double BM25_B = 0.75;
    private static final double MIN_SCORE = 0.5;

    private final String docsPath;
    private final List<DocumentChunk> chunks = new ArrayList<>();
    private double avgDocLen = 0;
    private int totalDocs = 0;
    private Map<String, Integer> df = new HashMap<>();

    public DocumentStore(@Value("${oncall.docs-path:../docs}") String docsPath) {
        this.docsPath = docsPath;
    }

    @PostConstruct
    public void loadDocuments() {
        Path dir = Paths.get(docsPath);
        if (!Files.exists(dir)) {
            log.warn("Docs directory not found: {} (cwd={})", dir.toAbsolutePath(), Paths.get(".").toAbsolutePath());
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .collect(Collectors.toList());
            files.forEach(this::loadFile);
            buildIndex();
            log.info("DocumentStore loaded: {} chunks from {} files (avgLen={})",
                    chunks.size(), files.size(), String.format("%.1f", avgDocLen));
        } catch (IOException e) {
            log.error("Failed to load docs from {}", dir, e);
        }
    }

    private void loadFile(Path file) {
        try {
            String content = Files.readString(file);
            String fileName = file.getFileName().toString();
            int idx = 0;
            
            // Split by Markdown headings (## or ### lines)
            String[] lines = content.split("\n");
            StringBuilder currentSection = new StringBuilder();
            String currentHeading = "";
            
            for (String line : lines) {
                String trimmed = line.trim();
                // Check if this line is a Markdown heading
                boolean isHeading = trimmed.startsWith("#") && !trimmed.startsWith("# ");
                if (trimmed.startsWith("##")) {
                    // Save previous section if non-empty
                    String sectionText = currentSection.toString().trim();
                    if (!sectionText.isEmpty()) {
                        String chunkId = fileName + "#" + (idx++);
                        String sectionName = currentHeading.isEmpty() ? fileName : currentHeading;
                        chunks.add(new DocumentChunk(chunkId, sectionText, fileName + " > " + sectionName, idx - 1));
                    }
                    // Start new section
                    currentHeading = trimmed.replaceAll("^#+\\s*", "").trim();
                    currentSection = new StringBuilder();
                    currentSection.append(line).append("\n");
                } else if (trimmed.startsWith("###")) {
                    // Sub-headings: include as part of current section with clear marker
                    currentSection.append("\n--- ").append(trimmed.replaceAll("^#+\\s*", "").trim()).append(" ---\n");
                } else {
                    currentSection.append(line).append("\n");
                    // If section grows too large, split by paragraph
                    if (currentSection.length() > 800 && trimmed.isEmpty()) {
                        String sectionText = currentSection.toString().trim();
                        if (!sectionText.isEmpty()) {
                            String chunkId = fileName + "#" + (idx++);
                            String sectionName = currentHeading.isEmpty() ? fileName : currentHeading;
                            chunks.add(new DocumentChunk(chunkId, sectionText, fileName + " > " + sectionName, idx - 1));
                        }
                        currentSection = new StringBuilder();
                        currentSection.append(line).append("\n");
                    }
                }
            }
            
            // Don't forget the last section
            String sectionText = currentSection.toString().trim();
            if (!sectionText.isEmpty()) {
                String chunkId = fileName + "#" + (idx++);
                String sectionName = currentHeading.isEmpty() ? fileName : currentHeading;
                chunks.add(new DocumentChunk(chunkId, sectionText, fileName + " > " + sectionName, idx - 1));
            }
            
            log.debug("Loaded {} chunks from {} (heading-based)", idx, fileName);
        } catch (IOException e) {
            log.error("Failed to read file: {}", file, e);
        }
    }

    private void buildIndex() {
        totalDocs = chunks.size();
        if (totalDocs == 0) return;
        avgDocLen = chunks.stream().mapToInt(c -> c.getContent().length()).average().orElse(1);
        df = new HashMap<>();
        for (DocumentChunk chunk : chunks) {
            Set<String> terms = tokenize(chunk.getContent());
            for (String term : terms) {
                df.merge(term, 1, Integer::sum);
            }
        }
    }

    public List<DocumentChunk> search(String query, int topK) {
        if (chunks.isEmpty()) return List.of();
        String[] queryTerms = tokenize(query).toArray(String[]::new);
        log.debug("Search query='{}', terms={}", query, Arrays.toString(queryTerms));

        return chunks.stream()
                .map(chunk -> new AbstractMap.SimpleEntry<>(chunk, bm25Score(queryTerms, chunk)))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .filter(e -> e.getValue() >= MIN_SCORE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double bm25Score(String[] queryTerms, DocumentChunk chunk) {
        String content = chunk.getContent();
        int docLen = content.length();
        Map<String, Integer> tf = new HashMap<>();
        for (String term : tokenize(content)) {
            tf.merge(term, 1, Integer::sum);
        }

        double score = 0;
        for (String term : queryTerms) {
            int termFreq = tf.getOrDefault(term, 0);
            int docFreq = df.getOrDefault(term, 1);
            double idf = Math.log(1 + (totalDocs - docFreq + 0.5) / (docFreq + 0.5));
            double numerator = termFreq * (BM25_K1 + 1);
            double denominator = termFreq + BM25_K1 * (1 - BM25_B + BM25_B * docLen / avgDocLen);
            score += idf * numerator / denominator;
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        String lower = text.toLowerCase();

        // Chinese: emit unigrams + bigrams for better coverage
        StringBuilder run = new StringBuilder();
        for (char c : lower.toCharArray()) {
            if (c >= 0x4e00 && c <= 0x9fff) {
                run.append(c);
            } else {
                emitChineseTokens(tokens, run);
            }
        }
        emitChineseTokens(tokens, run);

        // English: split by whitespace, keep words >= 2 chars
        String asciiPart = text.replaceAll("[^a-zA-Z0-9\\s]", " ");
        for (String word : asciiPart.toLowerCase().split("\\s+")) {
            word = word.trim();
            if (word.length() >= 2) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private void emitChineseTokens(Set<String> tokens, StringBuilder run) {
        if (run.length() == 0) return;
        if (run.length() == 1) {
            tokens.add(run.toString());
        } else {
            // unigrams
            for (int i = 0; i < run.length(); i++) {
                tokens.add(run.substring(i, i + 1));
            }
            // bigrams
            for (int i = 0; i <= run.length() - 2; i++) {
                tokens.add(run.substring(i, i + 2));
            }
        }
        run.setLength(0);
    }

    public int getChunkCount() { return chunks.size(); }

    public List<DocumentChunk> getChunks() { return new java.util.ArrayList<>(chunks); }

    public List<SearchResult> searchWithScores(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String[] queryTerms = tokenize(query).toArray(String[]::new);
        if (queryTerms.length == 0) {
            return List.of();
        }
        return chunks.stream()
                .map(chunk -> new AbstractMap.SimpleEntry<>(chunk, bm25Score(queryTerms, chunk)))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .filter(e -> e.getValue() >= MIN_SCORE)
                .map(e -> new SearchResult(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public String searchAsString(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        List<SearchResult> results = searchWithScores(query, 10);
        if (results == null || results.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (SearchResult sr : results) {
            if (sr.getScore() < MIN_SCORE) continue;
            if (count > 0) sb.append("\n---\n");
            DocumentChunk chunk = sr.getChunk();
            sb.append("[").append(chunk.getSource()).append("] ");
            sb.append(chunk.getContent());
            count++;
        }
        return sb.toString();
    }

}
