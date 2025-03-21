package com.adityachandel.booklore.convertor;

import com.adityachandel.booklore.model.dto.Sort;
import com.adityachandel.booklore.model.enums.SortDirection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SortConverter implements AttributeConverter<Sort, String> {

    @Override
    public String convertToDatabaseColumn(Sort sort) {
        if (sort == null) {
            return null;
        }
        return sort.getField() + "," + sort.getDirection().name();
    }

    @Override
    public Sort convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        String[] parts = dbData.split(",");
        Sort sort = new Sort();
        sort.setField(parts[0]);
        sort.setDirection(SortDirection.valueOf(parts[1]));
        return sort;
    }
}
