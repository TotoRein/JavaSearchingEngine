package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
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
        try {
            Page page = pageRepository.findById(id).orElseThrow(ChangeSetPersister.NotFoundException::new);
            return mapToDto(page);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Collection<PageDto> getAll() {
        return pageRepository.findAll().stream().map(PageCRUDServiceImpl::mapToDto).toList();
    }

    @Override
    public PageDto create(PageDto pageDto) {
        Page page = mapToEntity(pageDto);
        page.setSite(SiteCRUDServiceImpl.mapToEntity(siteCRUDService.getById(pageDto.getSiteId())));
        page = pageRepository.save(page);
        return mapToDto(page);
    }

    @Override
    public ResponseEntity<?> update(PageDto pageDto) {
        Page page = pageRepository.save(mapToEntity(pageDto));
        return ResponseEntity.ok(mapToDto(page));
    }

    @Override
    public ResponseEntity<?> deleteById(int id) {
        pageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public PageDto findBySiteIdAndPath(int siteId, String path) {
        return pageRepository.findBySiteIdAndPath(siteId, path).map(PageCRUDServiceImpl::mapToDto).orElse(null);
    }

    public boolean isPageInIndex(int siteId, String path) {
        return pageRepository.findBySiteIdAndPath(siteId, path).isPresent();
    }
}
