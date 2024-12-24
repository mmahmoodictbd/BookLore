package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.BookDTO;
import com.adityachandel.booklore.model.dto.BookViewerSettingDTO;
import com.adityachandel.booklore.model.dto.BookWithNeighborsDTO;
import com.adityachandel.booklore.model.dto.request.AssignShelvesRequest;
import com.adityachandel.booklore.model.dto.response.GoogleBooksMetadata;
import com.adityachandel.booklore.model.dto.request.SetMetadataRequest;
import com.adityachandel.booklore.service.BooksService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@RequestMapping("/v1/book")
@RestController
@AllArgsConstructor
public class BookController {

    private BooksService booksService;

    @GetMapping("/{bookId}")
    public ResponseEntity<BookDTO> getBook(@PathVariable long bookId) {
        return ResponseEntity.ok(booksService.getBook(bookId));
    }

    @GetMapping
    public ResponseEntity<List<BookDTO>> getBooks() {
        return ResponseEntity.ok( booksService.getBooks());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookDTO>> searchBooks(@RequestParam String title) {
        List<BookDTO> books = booksService.search(title);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{bookId}/cover")
    public ResponseEntity<Resource> getBookCover(@PathVariable long bookId) {
        return ResponseEntity.ok(booksService.getBookCover(bookId));
    }

    @GetMapping("/{bookId}/data")
    public ResponseEntity<byte[]> getBookData(@PathVariable long bookId) throws IOException {
        return booksService.getBookData(bookId);
    }

    @GetMapping("/{bookId}/viewer-setting")
    public ResponseEntity<BookViewerSettingDTO> getBookViewerSettings(@PathVariable long bookId) {
        return ResponseEntity.ok(booksService.getBookViewerSetting(bookId));
    }

    @PutMapping("/{bookId}/viewer-setting")
    public ResponseEntity<Void> updateBookViewerSettings(@RequestBody BookViewerSettingDTO bookViewerSettingDTO, @PathVariable long bookId) {
        booksService.saveBookViewerSetting(bookId, bookViewerSettingDTO);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{bookId}/update-last-read")
    public ResponseEntity<BookDTO> updateBookViewerSettings(@PathVariable long bookId) {
        return ResponseEntity.ok(booksService.updateLastReadTime(bookId));
    }

    @GetMapping("/{bookId}/fetch-metadata")
    public ResponseEntity<List<GoogleBooksMetadata>> getBookFetchMetadata(@PathVariable long bookId) {
        return ResponseEntity.ok(booksService.fetchProspectiveMetadataListByBookId(bookId));
    }

    @GetMapping("/fetch-metadata")
    public ResponseEntity<List<GoogleBooksMetadata>> fetchMedataByTerm(@RequestParam String term) {
        return ResponseEntity.ok(booksService.fetchProspectiveMetadataListBySearchTerm(term));
    }

    @PutMapping("/{bookId}/set-metadata")
    public ResponseEntity<Void> setBookMetadata(@RequestBody SetMetadataRequest setMetadataRequest, @PathVariable long bookId) {
        booksService.setMetadata(setMetadataRequest, bookId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign-shelves")
    public ResponseEntity<BookDTO> addBookToShelf(@RequestBody @Valid AssignShelvesRequest request) {
        return ResponseEntity.ok(booksService.addBookToShelf(request.getBookId(), request.getShelfIds()));
    }
}