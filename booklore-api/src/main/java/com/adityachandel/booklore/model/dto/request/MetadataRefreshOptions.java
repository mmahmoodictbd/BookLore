package com.adityachandel.booklore.model.dto.request;

import com.adityachandel.booklore.service.metadata.model.MetadataProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetadataRefreshOptions {
    @NotNull(message = "Default Provider cannot be null")
    private MetadataProvider allP1;
    private MetadataProvider allP2;
    private MetadataProvider allP3;
    private boolean refreshCovers;
    private boolean mergeCategories;
    private FieldOptions fieldOptions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOptions {
        private FieldProvider title;
        private FieldProvider description;
        private FieldProvider authors;
        private FieldProvider categories;
        private FieldProvider cover;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldProvider {
        private MetadataProvider p3;
        private MetadataProvider p2;
        private MetadataProvider p1;
    }
}
