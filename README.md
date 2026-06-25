# SpringBoot-RAG-ChatBot
A Retrieval Augmented Generation (RAG) based chatbot built with Spring Boot and Spring AI that lets users upload any PDF document, customize the AI behavior through a system prompt and ask questions about the document in a conversational way, powered by PGVector and OpenAI GPT-4o.

# What is RAG?
Instead of asking Chat GPT directly from its general knowledge, this application first reads the PDF, breaks it into smaller chunks and stores them in a PostgreSQL vector database. When you ask a question, it finds the most relevant chunks using semantic similarity search and passes them as context to GPT-4o. This means the AI answers based on the document, not general knowledge.

PDF Upload → Text Extraction → Chunking → Vector Embeddings → PGVector Store

User Question → Embedding → Similarity Search → Context + Question → GPT-4o → Answer

# Features
Upload any PDF document directly from the browser
Ask questions in plain English and get accurate answers from the document
Customize the AI system prompt to change behavior per use case
Smart duplicate detection — same PDF is never ingested twice

