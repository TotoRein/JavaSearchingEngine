package searchengine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.index.PageDto;
import searchengine.repositories.SiteRepository;
import searchengine.services.PageCRUDServiceImpl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
@Slf4j
public class SiteIndexer extends RecursiveTask<Integer> {
    private int rootSiteId;
    private String rootSiteUrl;
    private String currentPageUrl;

    /** todo: autowired не сработал, прокидывать каждый раз сервис - трата ресурсов. как исправить? */
    private final PageCRUDServiceImpl pageCRUDService;
    private final SiteRepository siteRepository;

    public SiteIndexer(String url, String root, int rootSiteId, PageCRUDServiceImpl pageCRUDService, SiteRepository siteRepository) {
        this.rootSiteId = rootSiteId;
        rootSiteUrl = root;
        currentPageUrl = url;
        this.pageCRUDService = pageCRUDService;
        this.siteRepository = siteRepository;
    }

    @Override
    protected Integer compute() {
        Integer quantity = 1;
        try {
            int delay = (int) (200 + Math.random() * 200);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Document doc;
        Connection.Response response;
        try {
            response = Jsoup.connect(currentPageUrl)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();
            doc = response.parse();
        } catch (IOException e) {
            log.error(e.getMessage());
            return 0;
        }

        // Для создания стартовой страницы обхода сайта
        if (isPageNotIndexed(rootSiteId, currentPageUrl)) {
            pageCRUDService.create(new PageDto(rootSiteId,
                cutRootUrl(currentPageUrl),
                response.statusCode(),
                doc.data())
            );
        } else {
            return 0;
        }

        Elements links = doc.select("a[href]");
        Iterator<Element> iterator = links.iterator();
        List<SiteIndexer> walkers = new ArrayList<>();
        HashSet<String> hrefSet = new HashSet<>();

        while (iterator.hasNext()) {
            Element element = iterator.next();
            String href = element.attr("abs:href").toLowerCase();
            if (href.length() <= 255) {
                hrefSet.add(href);
            }
        }
        for (String href : hrefSet) {
            if (href.contains(rootSiteUrl) & !href.contains("#")) {
                if (isPageNotIndexed(rootSiteId, href))  {
                    SiteIndexer walker = new SiteIndexer(href, rootSiteUrl, rootSiteId, pageCRUDService, siteRepository);
                    walker.fork();
                    walkers.add(walker);
                }
            }
        }

        for (SiteIndexer walker : walkers) {
            quantity += walker.join();
        }

        // В корневом потоке сайта/страницы ждём завершения всех прочих потоков
        if (currentPageUrl.equals(rootSiteUrl)) {
            log.info("Проиндексировано страниц: " + quantity);
            siteRepository.setIndexedStatusById(rootSiteId, new Date());
        }
        return quantity;
    }

    /**
     * Отрезает адрес корневой страницы
     *
     * @param baseUrl - обрезаемый url
     * @return путь от корневой страницы до текущей
     */
    private String cutRootUrl(String baseUrl) {
        return "/".concat(baseUrl.substring(rootSiteUrl.length()));
    }

    /**
     * Добавляет символ "/" в начало строки, если его нет
     *
     *
     * @param baseUrl проверяемы url
     * @return Строка, начинающаяся символом "/"
     */
    private String checkSlash(String baseUrl) {
        return baseUrl.startsWith("/") ? baseUrl : "/".concat(baseUrl);
    }

    /**
     * Проверяет, что страница ещё не проиндексирована
     *
     * @param siteId id индексируемого сайта в таблице Site
     * @param path адрес проверяемой страницы
     * @return true - страницы нет в индексе, false - страница есть в индексе
     */
    private boolean isPageNotIndexed(int siteId, String path) {
        return !pageCRUDService.isPageInIndex(siteId, checkSlash(cutRootUrl(path)));
    }
}
