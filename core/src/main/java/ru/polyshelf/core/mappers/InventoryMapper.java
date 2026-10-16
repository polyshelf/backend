package ru.polyshelf.core.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.models.InventoryItemEntity;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "name", source = "item", qualifiedByName = "resolveName")
    ProductResponse toProductResponse(InventoryItemEntity item);

    @Named("resolveName")
    default String resolveName(InventoryItemEntity item) {
        if (item.getCustomName() != null) {
            return item.getCustomName();
        }

        return item.getProduct() != null
                ? item.getProduct().getName()
                : null;
    }

}