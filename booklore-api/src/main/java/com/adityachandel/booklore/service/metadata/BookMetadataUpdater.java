package com.adityachandel.booklore.service.metadata;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.mapper.AuthorMapper;
import com.adityachandel.booklore.mapper.BookMetadataMapper;
import com.adityachandel.booklore.mapper.CategoryMapper;
import com.adityachandel.booklore.model.dto.BookMetadata;
import com.adityachandel.booklore.model.entity.*;
import com.adityachandel.booklore.repository.*;
import com.adityachandel.booklore.service.metadata.model.FetchedBookMetadata;
import com.adityachandel.booklore.util.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BookMetadataUpdater {

    private BookRepository bookRepository;
    private AuthorRepository authorRepository;
    private BookMetadataRepository bookMetadataRepository;
    private CategoryRepository categoryRepository;
    private FileService fileService;
    private BookMetadataMapper bookMetadataMapper;
    private AuthorMapper authorMapper;
    private CategoryMapper categoryMapper;
    private BookAwardRepository bookAwardRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BookMetadataEntity setBookMetadata(long bookId, FetchedBookMetadata newMetadata, boolean setThumbnail, boolean mergeCategories) {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        BookMetadataEntity metadata = bookEntity.getMetadata();

        updateLocks(newMetadata, metadata);

        if (Boolean.TRUE.equals(metadata.getAllFieldsLocked())) {
            log.warn("Attempted to update metadata for book with ID {}, but all fields are locked. No update performed.", bookId);
            return metadata;
        }

        if ((metadata.getTitleLocked() == null || !metadata.getTitleLocked()) && newMetadata.getTitle() != null) {
            metadata.setTitle(newMetadata.getTitle().isEmpty() ? null : newMetadata.getTitle());
        }

        if ((metadata.getSubtitleLocked() == null || !metadata.getSubtitleLocked()) && newMetadata.getSubtitle() != null) {
            metadata.setSubtitle(newMetadata.getSubtitle().isBlank() ? null : newMetadata.getSubtitle());
        }

        if ((metadata.getPublisherLocked() == null || !metadata.getPublisherLocked()) && newMetadata.getPublisher() != null) {
            metadata.setPublisher(newMetadata.getPublisher().isBlank() ? null : newMetadata.getPublisher());
        }

        if ((metadata.getPublishedDateLocked() == null || !metadata.getPublishedDateLocked()) && newMetadata.getPublishedDate() != null) {
            metadata.setPublishedDate(newMetadata.getPublishedDate());
        }

        if ((metadata.getLanguageLocked() == null || !metadata.getLanguageLocked()) && newMetadata.getLanguage() != null) {
            metadata.setLanguage(newMetadata.getLanguage().isBlank() ? null : newMetadata.getLanguage());
        }

        if ((metadata.getIsbn10Locked() == null || !metadata.getIsbn10Locked()) && newMetadata.getIsbn10() != null) {
            metadata.setIsbn10(newMetadata.getIsbn10().isBlank() ? null : newMetadata.getIsbn10());
        }

        if ((metadata.getIsbn13Locked() == null || !metadata.getIsbn13Locked()) && newMetadata.getIsbn13() != null) {
            metadata.setIsbn13(newMetadata.getIsbn13().isBlank() ? null : newMetadata.getIsbn13());
        }

        if ((metadata.getDescriptionLocked() == null || !metadata.getDescriptionLocked()) && newMetadata.getDescription() != null) {
            metadata.setDescription(newMetadata.getDescription().isBlank() ? null : newMetadata.getDescription());
        }

        if ((metadata.getPageCountLocked() == null || !metadata.getPageCountLocked()) && newMetadata.getPageCount() != null) {
            metadata.setPageCount(newMetadata.getPageCount());
        }

        if ((metadata.getRatingLocked() == null || !metadata.getRatingLocked()) && newMetadata.getRating() != null) {
            metadata.setRating(newMetadata.getRating());
        }

        if ((metadata.getReviewCountLocked() == null || !metadata.getReviewCountLocked()) && newMetadata.getReviewCount() != null) {
            metadata.setReviewCount(newMetadata.getReviewCount());
        }

        if ((metadata.getSeriesNameLocked() == null || !metadata.getSeriesNameLocked()) && newMetadata.getSeriesName() != null) {
            metadata.setSeriesName(newMetadata.getSeriesName());
        }

        if ((metadata.getSeriesNumberLocked() == null || !metadata.getSeriesNumberLocked()) && newMetadata.getSeriesNumber() != null) {
            metadata.setSeriesNumber(newMetadata.getSeriesNumber());
        }

        if ((metadata.getSeriesTotalLocked() == null || !metadata.getSeriesTotalLocked()) && newMetadata.getSeriesTotal() != null) {
            metadata.setSeriesTotal(newMetadata.getSeriesTotal());
        }

        if (newMetadata.getAwards() != null && !newMetadata.getAwards().isEmpty()) {
            HashSet<BookAwardEntity> newAwards = new HashSet<>();
            newMetadata.getAwards()
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(award -> {
                        boolean awardExists = bookMetadataRepository.findAwardByBookIdAndNameAndCategoryAndAwardedAt(
                                metadata.getBookId(),
                                award.getName(),
                                award.getCategory(),
                                award.getAwardedAt()) != null;

                        if (!awardExists) {
                            BookAwardEntity awardEntity = new BookAwardEntity();
                            awardEntity.setBook(metadata);
                            awardEntity.setName(award.getName());
                            awardEntity.setCategory(award.getCategory());
                            awardEntity.setDesignation(award.getDesignation());
                            awardEntity.setAwardedAt(award.getAwardedAt() != null ? award.getAwardedAt() : Instant.now().atZone(ZoneId.systemDefault()).toLocalDate());

                            newAwards.add(awardEntity);
                        }
                    });
            if (!newAwards.isEmpty()) {
                metadata.setAwards(new ArrayList<>(newAwards));
                bookAwardRepository.saveAll(newAwards);
            }
        }

        if ((metadata.getAuthorsLocked() == null || !metadata.getAuthorsLocked()) && newMetadata.getAuthors() != null && !newMetadata.getAuthors().isEmpty()) {
            metadata.setAuthors(newMetadata.getAuthors()
                    .stream()
                    .filter(a -> a != null && !a.isBlank())
                    .map(authorName -> authorRepository.findByName(authorName)
                            .orElseGet(() -> authorRepository.save(AuthorEntity.builder().name(authorName).build())))
                    .collect(Collectors.toList()));
        }

        if (mergeCategories) {
            if ((metadata.getCategoriesLocked() == null || !metadata.getCategoriesLocked()) && newMetadata.getCategories() != null) {
                HashSet<CategoryEntity> existingCategories = new HashSet<>(metadata.getCategories());
                newMetadata.getCategories()
                        .stream()
                        .filter(c -> c != null && !c.isBlank())
                        .forEach(categoryName -> {
                            CategoryEntity categoryEntity = categoryRepository.findByName(categoryName)
                                    .orElseGet(() -> categoryRepository.save(CategoryEntity.builder().name(categoryName).build()));
                            existingCategories.add(categoryEntity);
                        });
                metadata.setCategories(new ArrayList<>(existingCategories));
            }
        } else {
            if ((metadata.getCategoriesLocked() == null || !metadata.getCategoriesLocked()) && newMetadata.getCategories() != null && !newMetadata.getCategories().isEmpty()) {
                metadata.setCategories(newMetadata.getCategories()
                        .stream()
                        .filter(c -> c != null && !c.isBlank())
                        .map(categoryName -> categoryRepository.findByName(categoryName)
                                .orElseGet(() -> categoryRepository.save(CategoryEntity.builder().name(categoryName).build())))
                        .collect(Collectors.toList()));
            }
        }

        if (setThumbnail && (metadata.getThumbnailLocked() == null || !metadata.getThumbnailLocked()) && newMetadata.getThumbnailUrl() != null && !newMetadata.getThumbnailUrl().isEmpty()) {
            String thumbnailPath = null;
            try {
                thumbnailPath = fileService.createThumbnail(bookId, newMetadata.getThumbnailUrl());
                metadata.setCoverUpdatedOn(Instant.now());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            metadata.setThumbnail(thumbnailPath);
        }

        if (!metadata.getAuthors().isEmpty()) {
            authorRepository.saveAll(metadata.getAuthors());
        }
        if (!metadata.getCategories().isEmpty()) {
            categoryRepository.saveAll(metadata.getCategories());
        }

        bookMetadataRepository.save(metadata);
        return metadata;
    }

    private void updateLocks(FetchedBookMetadata newMetadata, BookMetadataEntity metadata) {
        if (newMetadata.getTitleLocked() != null) {
            metadata.setTitleLocked(newMetadata.getTitleLocked());
        }
        if (newMetadata.getSubtitleLocked() != null) {
            metadata.setSubtitleLocked(newMetadata.getSubtitleLocked());
        }
        if (newMetadata.getPublisherLocked() != null) {
            metadata.setPublisherLocked(newMetadata.getPublisherLocked());
        }
        if (newMetadata.getPublishedDateLocked() != null) {
            metadata.setPublishedDateLocked(newMetadata.getPublishedDateLocked());
        }
        if (newMetadata.getDescriptionLocked() != null) {
            metadata.setDescriptionLocked(newMetadata.getDescriptionLocked());
        }
        if (newMetadata.getIsbn13Locked() != null) {
            metadata.setIsbn13Locked(newMetadata.getIsbn13Locked());
        }
        if (newMetadata.getIsbn10Locked() != null) {
            metadata.setIsbn10Locked(newMetadata.getIsbn10Locked());
        }
        if (newMetadata.getPageCountLocked() != null) {
            metadata.setPageCountLocked(newMetadata.getPageCountLocked());
        }
        if (newMetadata.getLanguageLocked() != null) {
            metadata.setLanguageLocked(newMetadata.getLanguageLocked());
        }
        if (newMetadata.getRatingLocked() != null) {
            metadata.setRatingLocked(newMetadata.getRatingLocked());
        }
        if (newMetadata.getReviewCountLocked() != null) {
            metadata.setReviewCountLocked(newMetadata.getReviewCountLocked());
        }
        if (newMetadata.getSeriesNameLocked() != null) {
            metadata.setSeriesNameLocked(newMetadata.getSeriesNameLocked());
        }
        if (newMetadata.getSeriesNumberLocked() != null) {
            metadata.setSeriesNumberLocked(newMetadata.getSeriesNumberLocked());
        }
        if (newMetadata.getSeriesTotalLocked() != null) {
            metadata.setSeriesTotalLocked(newMetadata.getSeriesTotalLocked());
        }
        if (newMetadata.getAuthorsLocked() != null) {
            metadata.setAuthorsLocked(newMetadata.getAuthorsLocked());
        }
        if (newMetadata.getCategoriesLocked() != null) {
            metadata.setCategoriesLocked(newMetadata.getCategoriesLocked());
        }
        if (newMetadata.getCoverLocked() != null) {
            metadata.setCoverLocked(newMetadata.getCoverLocked());
        }
        if (newMetadata.getAllFieldsLocked() != null) {
            metadata.setAllFieldsLocked(newMetadata.getAllFieldsLocked());
        }
    }


    public BookMetadata updateMetadata(long bookId, BookMetadata updatedMetadata) {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        BookMetadataEntity metadata = bookEntity.getMetadata();

        List<String> lockedFields = new ArrayList<>();

        if (metadata.getAllFieldsLocked() != null && metadata.getAllFieldsLocked()) {
            log.warn("Attempted to update metadata for book with ID {}, but all fields are locked. No update performed.", bookId);
            throw ApiError.METADATA_LOCKED.createException(bookId);
        }

        if (metadata.getTitleLocked() && updatedMetadata.getTitle() != null) {
            lockedFields.add("Title");
        } else if (!metadata.getTitleLocked() && updatedMetadata.getTitle() != null) {
            metadata.setTitle(updatedMetadata.getTitle());
        }

        if (metadata.getSubtitleLocked() && updatedMetadata.getSubtitle() != null) {
            lockedFields.add("Subtitle");
        } else if (!metadata.getSubtitleLocked() && updatedMetadata.getSubtitle() != null) {
            metadata.setSubtitle(updatedMetadata.getSubtitle());
        }

        if (metadata.getPublisherLocked() && updatedMetadata.getPublisher() != null) {
            lockedFields.add("Publisher");
        } else if (!metadata.getPublisherLocked() && updatedMetadata.getPublisher() != null) {
            metadata.setPublisher(updatedMetadata.getPublisher());
        }

        if (metadata.getSeriesNameLocked() && updatedMetadata.getSeriesName() != null) {
            lockedFields.add("Series Name");
        } else if (!metadata.getSeriesNameLocked() && updatedMetadata.getSeriesName() != null) {
            metadata.setSeriesName(updatedMetadata.getSeriesName());
        }

        if (metadata.getSeriesNumberLocked() && updatedMetadata.getSeriesNumber() != null) {
            lockedFields.add("Series Number");
        } else if (!metadata.getSeriesNumberLocked() && updatedMetadata.getSeriesNumber() != null) {
            metadata.setSeriesNumber(updatedMetadata.getSeriesNumber());
        }

        if (metadata.getSeriesTotalLocked() && updatedMetadata.getSeriesTotal() != null) {
            lockedFields.add("Series Total");
        } else if (!metadata.getSeriesTotalLocked() && updatedMetadata.getSeriesTotal() != null) {
            metadata.setSeriesTotal(updatedMetadata.getSeriesTotal());
        }

        if (metadata.getPublishedDateLocked() && updatedMetadata.getPublishedDate() != null) {
            lockedFields.add("Published Date");
        } else if (!metadata.getPublishedDateLocked() && updatedMetadata.getPublishedDate() != null) {
            metadata.setPublishedDate(updatedMetadata.getPublishedDate());
        }

        if (metadata.getDescriptionLocked() && updatedMetadata.getDescription() != null) {
            lockedFields.add("Description");
        } else if (!metadata.getDescriptionLocked() && updatedMetadata.getDescription() != null) {
            metadata.setDescription(updatedMetadata.getDescription());
        }

        if (metadata.getIsbn13Locked() && updatedMetadata.getIsbn13() != null) {
            lockedFields.add("ISBN 13");
        } else if (!metadata.getIsbn13Locked() && updatedMetadata.getIsbn13() != null) {
            metadata.setIsbn13(updatedMetadata.getIsbn13());
        }

        if (metadata.getIsbn10Locked() && updatedMetadata.getIsbn10() != null) {
            lockedFields.add("ISBN 10");
        } else if (!metadata.getIsbn10Locked() && updatedMetadata.getIsbn10() != null) {
            metadata.setIsbn10(updatedMetadata.getIsbn10());
        }

        if (metadata.getPageCountLocked() && updatedMetadata.getPageCount() != null) {
            lockedFields.add("Page Count");
        } else if (!metadata.getPageCountLocked() && updatedMetadata.getPageCount() != null) {
            metadata.setPageCount(updatedMetadata.getPageCount());
        }

        if (metadata.getLanguageLocked() && updatedMetadata.getLanguage() != null) {
            lockedFields.add("Language");
        } else if (!metadata.getLanguageLocked() && updatedMetadata.getLanguage() != null) {
            metadata.setLanguage(updatedMetadata.getLanguage());
        }

        if (metadata.getRatingLocked() && updatedMetadata.getRating() != null) {
            lockedFields.add("Rating");
        } else if (!metadata.getRatingLocked() && updatedMetadata.getRating() != null) {
            metadata.setRating(updatedMetadata.getRating());
        }

        if (metadata.getReviewCountLocked() && updatedMetadata.getReviewCount() != null) {
            lockedFields.add("Review Count");
        } else if (!metadata.getReviewCountLocked() && updatedMetadata.getReviewCount() != null) {
            metadata.setReviewCount(updatedMetadata.getReviewCount());
        }

        if (metadata.getAuthorsLocked() && updatedMetadata.getAuthors() != null) {
            lockedFields.add("Authors");
        } else if (!metadata.getAuthorsLocked() && updatedMetadata.getAuthors() != null) {
            metadata.setAuthors(authorMapper.toAuthorEntityList(updatedMetadata.getAuthors()));
        }

        if (metadata.getCategoriesLocked() && updatedMetadata.getCategories() != null) {
            lockedFields.add("Categories");
        } else if (!metadata.getCategoriesLocked() && updatedMetadata.getCategories() != null) {
            metadata.setCategories(categoryMapper.toCategoryEntities(updatedMetadata.getCategories()));
        }

        if (!lockedFields.isEmpty()) {
            String lockedFieldsMessage = String.join(", ", lockedFields);
            log.error("Attempted to update the following locked fields for book with ID {}: {}", bookId, lockedFieldsMessage);
            throw ApiError.METADATA_LOCKED.createException(bookId);
        }

        bookRepository.save(bookEntity);
        return bookMetadataMapper.toBookMetadata(metadata, false);
    }
}