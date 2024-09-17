package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteCRUDServiceImpl implements CRUDService<SiteDto> {
    private final SiteRepository repository;

    public static SiteDto mapToDto(Site site) {
        return new SiteDto(
                site.getId(),
                site.getStatus(),
                site.getStatusTime(),
                site.getLastError(),
                site.getUrl(),
                site.getName(),
                /** todo: список стриниц передавать тоже */
                List.of()//site.getPages().stream().map(PageCRUDServiceImpl::mapToDto).toList()
        );
    }

    public static Site mapToEntity(SiteDto siteDto) {
        return new Site(
                siteDto.getId(),
                siteDto.getStatus(),
                siteDto.getStatusTime(),
                siteDto.getLastError(),
                siteDto.getUrl(),
                siteDto.getName(),
                siteDto.getPages().stream().map(PageCRUDServiceImpl::mapToEntity).toList()
        );
    }

    /** todo: сделать нормальный формат возвращаемого значения*/
    @Override
    public SiteDto getById(int id) {
        try {
            log.info("Search for id:" + id);
            return mapToDto(repository.findById(id).orElseThrow());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public Collection<SiteDto> getAll() {
        return repository.findAll().stream().map(SiteCRUDServiceImpl::mapToDto).toList();
    }

    @Override
    public SiteDto create(SiteDto siteDto) {
        Site site = new Site(siteDto.getUrl(), siteDto.getName());
        return mapToDto(repository.save(site));
    }

    @Override
    public ResponseEntity<?> update(SiteDto siteDto) {
        Site site;
        try {
            site = repository.findById(siteDto.getId()).orElseThrow();
        } catch (NoSuchElementException ex) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        if (siteDto.getStatus() != null) {
            site.setStatus(siteDto.getStatus());
            site.setStatusTime(new Date());
        }

        if (siteDto.getLastError() != null) {
            site.setLastError(siteDto.getLastError());
        }

        repository.save(site);
        return new ResponseEntity<>(mapToDto(site), HttpStatus.OK);

    }

    @Override
    public ResponseEntity<?> deleteById(int id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
