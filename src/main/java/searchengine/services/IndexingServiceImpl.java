package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.LemmaFinder;
import searchengine.SiteIndexer;
import searchengine.config.SitesList;
import searchengine.dto.index.*;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.Status;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesList;

    private final PageCRUDService pageCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    private final SiteCRUDService siteCRUDService;
    private ForkJoinPool indexingThreadsPool = new ForkJoinPool();

    @Override
    public IndexingResponse startIndexing() {
       /*
         * Найти сайт
         * Если индексируется -> сообщение и конец
         * Если Индексирован / ошибка - удалить и начать индексацию
         * */
       indexingThreadsPool = new ForkJoinPool();
       for (searchengine.config.Site site : sitesList.getSites()) {
            /* Если есть в БД и... */
            Site currentSite = siteCRUDService.findByUrl(site.getUrl());
            if (currentSite != null) {
                /* ...уже индексируется */
                if (currentSite.getStatus().equals(Status.INDEXING)) {
                    return new IndexingResponse(false, "Сайт "+ currentSite.getUrl() + " уже индексируется!");
                }
                /* ...не индексируется */
                siteCRUDService.deleteById(currentSite.getId());
            }

            log.info("Сайт " + site.getUrl() + " взят в обработку");

            /* Создаём новый сайт */
            Site siteEntity = new Site(site.getUrl(), site.getName());
            siteEntity.updateStatus(Status.INDEXING);
            SiteDto siteDto = siteCRUDService.create(SiteCRUDService.mapToDto(siteEntity));
            int siteId = siteDto.getId();

            new Thread(() -> indexingThreadsPool.invoke(new SiteIndexer(site.getUrl(),
                    site.getUrl(),
                    siteId,
                    pageCRUDService,
                    siteCRUDService,
                    lemmaCRUDService,
                    indexCRUDService))).start();
       }

        return new IndexingResponse(true, null);
    }

    @Override
    public ResponseEntity<?> stopIndexing() {
        log.info("Остановка индексации");
        indexingThreadsPool.shutdown();
        try {
            Collection<Site> sites = siteCRUDService.findByStatus(Status.INDEXING).orElseThrow(ChangeSetPersister.NotFoundException::new);
            if (sites.isEmpty()) {
                return ResponseEntity.badRequest().body(new IndexingResponse(false, "Индексация не запущена"));
            }
            for (Site site : sites) {
                siteCRUDService.setFailedStatusById(site.getId(), new Date());
            }
            return ResponseEntity.ok(new IndexingResponse(true, null));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Возникла ошибка " + exception.getMessage());
        }
    }

    /**
     * Находит сайт в бд, удаляет страницу, получает контент страницы, записывает её в бд, создаёт индекс по ней
     * */
    @Override
    public IndexingResponse indexPage(String url) {
        log.info("Начало индексации отдельной страницы " + url);


        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        /* Проверяем есть ли сайт в конфиге и бд */
        Site site = null;
        for (searchengine.config.Site configSite : sitesList.getSites()) {
            if (url.contains(configSite.getUrl())) {
                site = siteCRUDService.findByUrl(configSite.getUrl());

                if (site == null) {
                    site = new Site(configSite.getUrl(), configSite.getName());
                    site.updateStatus(Status.INDEXED);
                    siteCRUDService.update(SiteCRUDService.mapToDto(site));
                }
                break;
            }
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        /* Удаляем страницу, есть есть в бд */
        String pageUrl = url.substring(site.getUrl().length() - 1);
        /* Удаляем информацию о странице из indexes и lemmas */
        Page pageEntity = pageCRUDService.findBySiteAndPath(site.getId(), pageUrl);
        if (pageEntity != null) {
            Collection<IndexDto> indexesForPage = indexCRUDService.getIndexesByPageId(pageEntity.getId());
            for (IndexDto indexDto : indexesForPage) {
                LemmaDto lemmaDto = lemmaCRUDService.getById(indexDto.getLemmaId());
                if (lemmaDto.getFrequency() == 1) {
                    lemmaCRUDService.deleteById(lemmaDto.getId());
                } else {
                    lemmaDto.setFrequency(lemmaDto.getFrequency() - 1);
                    lemmaCRUDService.update(lemmaDto);
                }
                indexCRUDService.deleteById(indexDto.getId());
            }
            pageCRUDService.deleteById(pageEntity.getId());
        }

        /* Получаем контент и создаём страницу */
        Document doc;
        Connection.Response response;
        try {
            response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();
            doc = response.parse();
        } catch (IOException e) {
            log.error("Не получилось получить content! " + e.getMessage());
            return new IndexingResponse(false, "При соединении со страницей возникла ошибка.");
        }

        if (doc.body().html().isEmpty()) {
            return new IndexingResponse(false, "Задана страница без содержимого.");
        }

        PageDto pageDto = pageCRUDService.create(
            new PageDto(site.getId(),
                    pageUrl,
                    response.statusCode(),
                    doc.html()
            )
        );

        /* Создаёт индекс по странице */
        log.info("Начало создания индекса страницы " + pageUrl);
        int result = generateIndexForPage(pageDto);
        log.info("Индекс создан " + pageUrl);
        String responseMessage = switch (result) {
            case 0 -> "При создании индекса произошла ошибка";
            case 1 -> "Индекс успешно создан";
            default -> "";
        };

        log.info("Отдельная страница проиндексирована " + pageUrl);

        return new IndexingResponse(true, responseMessage);
    }

    /**
     * Получает на вход dto страницы, разбивает content на леммы, записывает их в lemma и frequency
     */
    public int generateIndexForPage(PageDto pageDto) {
        String pageText = pageDto.getContent();
        LemmaFinder lemmaFinder;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException exception) {
            log.error("Не получилось получить LemmaFinder! " + exception.getMessage());
            return 0;
        }
        Map<String, Integer> lemmasWithFrequencies = lemmaFinder.collectLemmas(pageText);

        for (String lemmaKey : lemmasWithFrequencies.keySet()) {
            LemmaDto lemmaDto = lemmaCRUDService.getByLemma(lemmaKey);
            if (lemmaDto != null) {
                lemmaDto.setFrequency(lemmaDto.getFrequency() + 1);
                lemmaCRUDService.update(lemmaDto);
            } else {
                lemmaDto = new LemmaDto(pageDto.getSiteId(), lemmaKey, 1);
                lemmaDto = lemmaCRUDService.create(lemmaDto);
            }
            IndexDto indexDto = new IndexDto(pageDto.getId(), lemmaDto.getId(), lemmasWithFrequencies.get(lemmaKey));
            indexCRUDService.create(indexDto);
        }
        return 1;
    }

}
