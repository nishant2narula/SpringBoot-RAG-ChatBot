package com.SpringAI.ChatBotClient.service;

import com.SpringAI.ChatBotClient.entity.IngestedDocument;
import com.SpringAI.ChatBotClient.repository.IngestedDocumentRepository;
import org.springframework.ai.vectorstore.VectorStore;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;

@Service
public class PdfIngestionService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final IngestedDocumentRepository ingestedDocumentRepository;

    @Value("${app.pdf.path}")
    private Resource pdfResource;

    public PdfIngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate, IngestedDocumentRepository ingestedDocumentRepository) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.ingestedDocumentRepository = ingestedDocumentRepository;
    }

    @PostConstruct
    public void ingestPdf() {

        try {
            String fileName = pdfResource.getFilename();
            String fileHash = generateHash(pdfResource);

            // Check by hash — catches duplicate files even with different names
            if (ingestedDocumentRepository.existsByFileHash(fileHash)) {
                System.out.println("⏭️ PDF already ingested: " + fileName + ". Skipping.");
                return;
            }

            System.out.println("New PDF detected: " + fileName + ". Starting ingestion...");
            ingest(fileName, fileHash);

        } catch (Exception e) {
            System.out.println("Error during ingestion: " + e.getMessage());
        }
    }

    private void ingest(String fileName, String fileHash) {

        // 1. Read PDF
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                pdfResource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1)
                        .build()
        );

        // 2. Split into chunks
        TextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
        List<Document> documents = splitter.transform(pdfReader.read());

        if (documents.isEmpty()) {
            System.out.println(" No documents extracted from PDF!");
            return;
        }
        // 3. Store in vector store
        vectorStore.add(documents);

        // Track the ingested document
        IngestedDocument ingestedDocument = new IngestedDocument();
        ingestedDocument.setFileName(fileName);
        ingestedDocument.setFileHash(fileHash);
        ingestedDocumentRepository.save(ingestedDocument);
        System.out.println("PDF ingested: " + documents.size() + " chunks stored.");
    }

    private String generateHash(Resource resource) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = resource.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
