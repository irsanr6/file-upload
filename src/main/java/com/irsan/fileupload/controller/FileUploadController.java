package com.irsan.fileupload.controller;

import com.irsan.fileupload.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@RestController
@RequestMapping("api/files")
@Slf4j
public class FileUploadController {

    @Value("${upload-dir}")
    private String uploadDir;

    @PostMapping("upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename(), "File is null");

        try {
            Path directory = Paths.get(uploadDir);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(originalFileName).normalize();
            log.info("File path: {}", filePath);
            Files.write(filePath, file.getBytes());

            return ResponseEntity.ok(String.format("File uploaded successfully: %s", originalFileName));
        } catch (IOException e) {
            throw new FileStorageException(String.format("Failed to store file: %s", originalFileName), e);
        }
    }

    @GetMapping("download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            log.info("Download file path: {}", fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException(String.format("Could not read file: %s", fileName));
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s", resource.getFilename()))
                    .body(resource);
        } catch (IOException e) {
            throw new FileStorageException(String.format("Failed to download file: %s", fileName), e);
        }
    }

    @DeleteMapping("delete/{fileName:.+}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            log.info("Delete file path: {}", filePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);

                return ResponseEntity.ok(String.format("File deleted successfully: %s", fileName));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("File not found: %s", fileName));
            }
        } catch (IOException e) {
            throw new FileStorageException(String.format("Failed to delete file: %s", fileName), e);
        }
    }

}