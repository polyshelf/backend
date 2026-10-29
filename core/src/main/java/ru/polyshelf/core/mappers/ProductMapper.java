package ru.polyshelf.core.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.polyshelf.core.dto.OffProductDto;
import ru.polyshelf.core.models.ProductEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "barcode", source = "code")
    @Mapping(target = "name", source = "productName")
    ProductEntity toProductEntity(OffProductDto item);

    default String map(List<OffProductDto.LocalizedText> names) {
        if (names == null) {
            return null;
        }

        return names.stream()
                .filter(n -> "main".equals(n.lang()) && n.text() != null)
                .map(text -> text.text().substring(0, Math.min(text.text().length(), 255)))
                .findFirst()
                .orElse(null);
    }
}