package com.adityachandel.booklore.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BookMetadataDTO {
    private Long bookId;
    private String googleBookId;
    private String amazonBookId;
    private String title;
    private String subtitle;
    private String publisher;
    private LocalDate publishedDate;
    private String description;
    private String isbn13;
    private String isbn10;
    private Integer pageCount;
    private String language;
    private Float rating;
    private Integer reviewCount;
    private List<AuthorDTO> authors;
    private List<CategoryDTO> categories;

}
