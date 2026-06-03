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
            int start = 0;
            int idx = 0;
            while (start < content.length()) {
                int end = Math.min(start + CHUNK_SIZE, content.length());
                String chunkText = content.substring(start, end).trim();
                if (!chunkText.isEmpty()) {
                    chunks.add(new DocumentChunk(
                            fileName + "#" + (idx++),
                            chunkText,
                            fileName,
                            idx - 1
                    ));
                }
                start += CHUNK_SIZE - CHUNK_OVERLAP;
            }
            log.debug("Loaded {} chunks from {}", idx, fileName);
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
                .filter(e -> e.getValue() > 0)
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

        // Collect consecutive Chinese chars into runs, then emit bigrams
        StringBuilder run = new StringBuilder();
        for (char c : lower.toCharArray()) {
            if (c >= 0x4e00 && c <= 0x9fff) {
                run.append(c);
            } else {
                emitBigrams(tokens, run);
            }
        }
        emitBigrams(tokens, run);

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

    private void emitBigrams(Set<String> tokens, StringBuilder run) {
        if (run.length() == 1) {
            tokens.add(run.toString());
        } else if (run.length() >= 2) {
            for (int i = 0; i <= run.length() - 2; i++) {
                tokens.add(run.substring(i, i + 2));
            }
        }
        run.setLength(0);
    }

    public int getChunkCount() { return chunks.size(); }
}
