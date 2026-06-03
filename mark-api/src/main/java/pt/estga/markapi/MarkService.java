package pt.estga.markapi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.estga.mark.dtos.MarkDto;
import pt.estga.mark.dtos.MarkUpdateDto;

public interface MarkService {

    Page<MarkDto> findAll(Pageable pageable);

    MarkDto findById(Long id);

    MarkDto update(Long id, MarkUpdateDto dto);

    void deleteById(Long id);
}
