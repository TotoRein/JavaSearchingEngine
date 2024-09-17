package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.index.PageDto;
import searchengine.model.Page;
import searchengine.repositories.PageRepository;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class PageCRUDServiceImpl implements CRUDService<PageDto> {
    private final SiteCRUDServiceImpl siteCRUDService;
    private final PageRepository pageRepository;

    public static PageDto mapToDto(Page page) {
        return new PageDto(page.getId(), page.getSite().getId(), page.getPath(), page.getCode(), page.getContent());
    }

    public static Page mapToEntity(PageDto pageDto) {
        return new Page(
                pageDto.getId(),
                null,
                pageDto.getPath(),
                pageDto.getCode(),
                pageDto.getContent());
    }

    @Override
    public PageDto getById(int id) {
        return null;
    }

    @Override
    public Collection<PageDto> getAll() {
        return null;
    }

    @Override
    public PageDto create(PageDto pageDto) {
        Page page = mapToEntity(pageDto);
        page.setSite(SiteCRUDServiceImpl.mapToEntity(siteCRUDService.getById(pageDto.getSiteId())));
        page = pageRepository.save(page);
        return mapToDto(page);
    }

    @Override
    public ResponseEntity<?> update(PageDto item) {
        return null;
    }

    @Override
    public ResponseEntity<?> deleteById(int id) {
        return null;
    }
}
