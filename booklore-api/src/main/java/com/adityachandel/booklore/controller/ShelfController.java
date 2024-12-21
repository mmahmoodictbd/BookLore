package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.ShelfDTO;
import com.adityachandel.booklore.model.dto.request.ShelfCreateRequest;
import com.adityachandel.booklore.service.ShelfService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping(("/v1/shelf"))
public class ShelfController {

    private final ShelfService shelfService;

    @GetMapping
    public ResponseEntity<List<ShelfDTO>> findAll() {
        return ResponseEntity.ok(shelfService.getShelves());
    }

    @GetMapping("/{shelfId}")
    public ResponseEntity<ShelfDTO> findById(@PathVariable Long shelfId) {
        return ResponseEntity.ok(shelfService.getShelf(shelfId));
    }

    @PostMapping
    public ResponseEntity<ShelfDTO> createShelf(@Valid @RequestBody ShelfCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shelfService.createShelf(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShelfDTO> updateShelf(@PathVariable Long id, @Valid @RequestBody ShelfCreateRequest request) {
        return ResponseEntity.ok(shelfService.updateShelf(id, request));
    }

    @PostMapping("/{shelfId}/book/{bookId}")
    public ResponseEntity<ShelfDTO> addBookToShelf(@PathVariable Long shelfId, @PathVariable Long bookId) {
        return ResponseEntity.ok(shelfService.addBookToShelf(shelfId, bookId));
    }

    @DeleteMapping("/{shelfId}")
    public ResponseEntity<Void> deleteShelf(@PathVariable Long shelfId) {
        shelfService.deleteShelf(shelfId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
