package pt.estga.mark.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import pt.estga.mark.entities.MarkCategory;

@Mapper(componentModel = "spring")
public interface MarkCategoryMapper {

    void update(MarkCategory source, @MappingTarget MarkCategory target);

}
