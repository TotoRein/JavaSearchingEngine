package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.index.PageDto;
import searchengine.model.Page;
import searchengine.repositories.PageRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PageCRUDService implements CRUDService<PageDto> {
    private final SiteCRUDService siteCRUDService;
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
                pageDto.getContent(),
                new ArrayList<>());
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
        return pageRepository.findAll().stream().map(PageCRUDService::mapToDto).toList();
    }

    public List<Page> getAllById(List<Integer> ids) {
        return pageRepository.findAllById(ids);
    }

    @Override
    public PageDto create(PageDto pageDto) {
        Page page = mapToEntity(pageDto);
        page.setSite(SiteCRUDService.mapToEntity(siteCRUDService.getById(pageDto.getSiteId())));
        try {
            page = pageRepository.saveAndFlush(page);
            return mapToDto(page);
        } catch (Exception exception) {
            log.error("Attempt to insert something wrong! Path: " + page.getPath() + "\n" +
                    exception.getMessage() + pageDto);
            return null;
        }
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

    public boolean isPageInIndex(int siteId, String path) {
        return pageRepository.findBySiteIdAndPath(siteId, path).isPresent();
    }

    public Page findBySiteAndPath(int siteId, String path) {
        return pageRepository.findBySiteIdAndPath(siteId, path).orElse(null);
    }

    public List<Page> getPagesBySite(int siteId) {
        return pageRepository.findBySiteId(siteId);
    }
}
