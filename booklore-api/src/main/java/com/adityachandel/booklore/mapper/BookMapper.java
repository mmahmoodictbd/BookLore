package com.adityachandel.booklore.mapper;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.entity.AuthorEntity;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.CategoryEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {BookMetadataMapper.class, ShelfMapper.class})
public interface BookMapper {

    @Mapping(source = "library.id", target = "libraryId")
    @Mapping(source = "metadata", target = "metadata")
    @Mapping(source = "shelves", target = "shelves")
    Book toBook(BookEntity bookEntity);

    @Mapping(source = "library.id", target = "libraryId")
    @Mapping(source = "metadata", target = "metadata")
    @Mapping(source = "shelves", target = "shelves")
    Book toBookWithDescription(BookEntity bookEntity, @Context boolean includeDescription);

    default List<String> mapAuthors(List<AuthorEntity> authors) {
        if (authors == null) {
            return null;
        }
        return authors.stream()
                .map(AuthorEntity::getName) // Assuming 'name' is the field in AuthorEntity
                .collect(Collectors.toList());
    }

    // Map List<CategoryEntity> to List<String> (category names)
    default List<String> mapCategories(List<CategoryEntity> categories) {
        if (categories == null) {
            return null;
        }
        return categories.stream()
                .map(CategoryEntity::getName) // Assuming 'name' is the field in CategoryEntity
                .collect(Collectors.toList());
    }
}