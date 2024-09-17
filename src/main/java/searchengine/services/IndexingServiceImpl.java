package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.SiteIndexer;
import searchengine.model.Site;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexingResponse;
import searchengine.model.enums.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageCRUDServiceImpl pageCRUDService;

    @Override
    public IndexingResponse startIndexing() {
        for (searchengine.config.Site site : sitesList.getSites()) {
            Site siteEntity = new Site(site.getUrl(), site.getName());
            siteEntity.setStatus(Status.INDEXING);
            siteEntity.setStatusTime(new Date());
            siteEntity = siteRepository.save(siteEntity);
            HashSet<String> urlSet = new ForkJoinPool().invoke(new SiteIndexer(site.getUrl(), site.getUrl(), siteEntity.getId(), pageCRUDService));
        }

        return new IndexingResponse(true, "some response for now");
    }

}
