package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.service.FileUploadService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Book> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("libraryId") long libraryId, @RequestParam("pathId") long pathId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is missing.");
        }
        return ResponseEntity.ok(fileUploadService.uploadFile(file, libraryId, pathId));
    }
}
