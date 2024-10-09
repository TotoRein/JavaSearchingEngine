package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.SiteIndexer;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.enums.Status;
import searchengine.repositories.SiteRepository;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageCRUDServiceImpl pageCRUDService;
    private ForkJoinPool indexingThreadsPool = new ForkJoinPool();

    @Override
    public IndexingResponse startIndexing() {
        /**
         * Найти сайт
         * Если индексируется -> сообщение и конец
         * Если Индексирован / фейлед - удалить и начать индексацию
         * */
        Integer result = 0;
        for (searchengine.config.Site site : sitesList.getSites()) {
            /* Если есть в БД и... */
            Site currentSite = siteRepository.findByUrl(site.getUrl());
            if (currentSite != null) {
                /* ...уже индексируется */
                if (currentSite.getStatus().equals(Status.INDEXING)) {
                    return new IndexingResponse(false, "Сайт + "+ currentSite.getUrl() + " уже индексируется!");
                }
                /* ...не индексируется */
                siteRepository.delete(currentSite);
            }

            /* Создаём новый сайт */
            Site siteEntity = new Site(site.getUrl(), site.getName());
            siteEntity.updateStatus(Status.INDEXING);
            siteEntity = siteRepository.save(siteEntity);
            siteRepository.flush();

            /** todo: Как инициализировать pageCRUD и repository через autowired, а не передавать в каждый поток? */
            indexingThreadsPool.invoke(new SiteIndexer(site.getUrl(), site.getUrl(), siteEntity.getId(), pageCRUDService, siteRepository));
        }

        return new IndexingResponse(true, null);
    }

    @Override
    public ResponseEntity<?> stopIndexing() {
        indexingThreadsPool.shutdown();
        try {
            Collection<Site> sites = siteRepository.findByStatus(Status.INDEXING).orElseThrow(ChangeSetPersister.NotFoundException::new);
            if (sites.isEmpty()) {
                return ResponseEntity.badRequest().body(new IndexingResponse(false, "Индексация не запущена"));
            }
            for (Site site : sites) {
                siteRepository.setFailedStatusById(site.getId(), new Date());
            }
            return ResponseEntity.ok(new IndexingResponse(true, null));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Возникла ошибка " + exception.getMessage());
        }
    }
}
