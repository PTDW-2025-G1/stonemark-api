package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.mark.repositories.MarkCategoryRepository;

@Service
@RequiredArgsConstructor
public class MarkCategoryQueryService {

    private final MarkCategoryRepository repository;

}
