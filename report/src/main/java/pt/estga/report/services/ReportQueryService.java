package pt.estga.report.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import pt.estga.filterutils.QueryProcessor;
import pt.estga.filterutils.models.PagedRequest;
import pt.estga.filterutils.models.QueryResult;
import pt.estga.filterutils.SpecificationBuilder;
import pt.estga.report.dtos.ReportResponseDto;
import pt.estga.report.entities.Report;
import pt.estga.report.mappers.ReportMapper;
import pt.estga.report.repositories.ReportRepository;


@Service
@RequiredArgsConstructor
public class ReportQueryService {

	private final ReportRepository repository;
	private final SpecificationBuilder<Report> specificationBuilder;
	private final ReportMapper mapper;

	public Page<ReportResponseDto> search(PagedRequest request) {
		QueryProcessor<Report> processor = new QueryProcessor<>(specificationBuilder);
		QueryResult<Report> result = processor.process(request);
		Page<Report> page = (result.specification() == null)
				? repository.findAll(result.pageable())
				: repository.findAll(result.specification(), result.pageable());
		return page.map(mapper::toDto);
	}
}
