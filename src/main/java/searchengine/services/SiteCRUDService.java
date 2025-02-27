package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.index.SiteDto;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repositories.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteCRUDService implements CRUDService<SiteDto> {
    private final SiteRepository repository;

    public static SiteDto mapToDto(Site site) {
        return new SiteDto(
                site.getId(),
                site.getStatus(),
                site.getStatusTime(),
                site.getLastError(),
                site.getUrl(),
                site.getName(),
                new ArrayList<>()
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
                siteDto.getPages().stream().map(PageCRUDService::mapToEntity).toList()
        );
    }

    @Override
    public SiteDto getById(int id) {
        try {
            return mapToDto(repository.findById(id).orElseThrow(ChangeSetPersister.NotFoundException::new));
        } catch (IllegalArgumentException ex) {
            log.error("Для поиска сайта задан пустой id.");
            return null;
        } catch (ChangeSetPersister.NotFoundException ex) {
            log.error("Попытка найти сайт с несуществующим id (" + id + ").");
            Site site = repository.findById(id).get();
            log.info(site.toString());
            return null;
        }
    }

    @Override
    public Collection<SiteDto> getAll() {
        return repository.findAll().stream().map(SiteCRUDService::mapToDto).toList();
    }

    @Override
    public SiteDto create(SiteDto siteDto) {
        Site site = new Site(siteDto.getUrl(), siteDto.getName(), siteDto.getStatus(), siteDto.getStatusTime());
        site = repository.saveAndFlush(site);
        return mapToDto(site);
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

    public Site findByUrl(String url) {
        return repository.findByUrl(url);
    }

    public void setIndexedStatusById(int id, Date statusTime) {
        repository.setIndexedStatusById(id, statusTime);
    }
    public void setFailedStatusById(int id, Date statusTime) {
        repository.setFailedStatusById(id, statusTime);
    }

    public Optional<Collection<Site>> findByStatus(Status status) {
        return repository.findByStatus(status);
    }

    public void updateStatusTime(int siteId) {
        repository.updateStatusTime(siteId, new Date());
    }
}
