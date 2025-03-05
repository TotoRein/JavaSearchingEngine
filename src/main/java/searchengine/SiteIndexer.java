package searchengine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.index.IndexDto;
import searchengine.dto.index.LemmaDto;
import searchengine.dto.index.PageDto;
import searchengine.services.IndexCRUDService;
import searchengine.services.LemmaCRUDService;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;

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
    /** Не сработал, потому что не является бином, а бином сделать recursiveTask нельзя, сделать отдельный класс для конфига */
    private final PageCRUDService pageCRUDService;
    private final SiteCRUDService siteCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;

    public  SiteIndexer(String url,
                       String root,
                       int rootSiteId,
                       PageCRUDService pageCRUDService,
                       SiteCRUDService siteCRUDService,
                       LemmaCRUDService lemmaCRUDService,
                       IndexCRUDService indexCRUDService) {
        this.rootSiteId = rootSiteId;
        rootSiteUrl = root;
        currentPageUrl = url;
        this.pageCRUDService = pageCRUDService;
        this.siteCRUDService = siteCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexCRUDService = indexCRUDService;
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

        /* Если пустой content, пропускаем страницу  */
        if (doc.data().isEmpty()) {
            return 0;
        }
        PageDto pageDto;
        if (isPageNotIndexed(rootSiteId, currentPageUrl)) {
            pageDto = pageCRUDService.create(new PageDto(rootSiteId,
                cutRootUrl(currentPageUrl),
                response.statusCode(),
                doc.html())
            );
        } else {
            return 0;
        }

        /* Обновляем timestamp статуса */
        siteCRUDService.updateStatusTime(rootSiteId);

        Elements links = doc.select("a[href]");
        Iterator<Element> iterator = links.iterator();
        List<SiteIndexer> walkers = new ArrayList<>();
        HashSet<String> hrefSet = new HashSet<>();

        while (iterator.hasNext()) {
            Element element = iterator.next();
            String href = element.attr("abs:href").toLowerCase();
            hrefSet.add(href);
        }

        /* Обрабатываем каждую ссылку на странице, запускаем по корректным SiteIndexer в новом потоке */
        for (String href : hrefSet) {
            href = href.split("\\?")[0];
            href = href.endsWith("/") ? href.substring(0, href.length() - 1) : href;
            if (href.contains(rootSiteUrl) & !href.contains("#")) {
                if (isPageNotIndexed(rootSiteId, href)) {

                    /* Проверка корректность ссылок */
                    // Без бина не подтянуть значение из конфига, а бин создать нельзя
                    int maxPathLength = 255;
                    if (href.length() > maxPathLength) {
                        continue;
                    }
                    // Игнорируем картинки
                    String[] wrongExtensions = {".png", ".jpg", ".jpeg", ".svg", ".gif", ".pdf"};
                    if (Arrays.stream(wrongExtensions).anyMatch(href::endsWith)) {
                        continue;
                    }

                    SiteIndexer walker = new SiteIndexer(href,
                            rootSiteUrl,
                            rootSiteId,
                            pageCRUDService,
                            siteCRUDService,
                            lemmaCRUDService,
                            indexCRUDService);
                    walker.fork();
                    walkers.add(walker);
                }
            }
        }

        /* Создаём индекс по странице */
        generateIndexForPage(pageDto);

        for (SiteIndexer walker : walkers) {
            quantity += walker.join();
        }

        /* В корневом потоке сайта/страницы ждём завершения всех прочих потоков */
        if (currentPageUrl.equals(rootSiteUrl)) {
            log.info("Проиндексировано страниц: " + quantity);
            siteCRUDService.setIndexedStatusById(rootSiteId, new Date());
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

    /**
     * Получает на вход dto страницы, разбивает content на леммы, записывает их в lemma и frequency
     */
    public int generateIndexForPage(PageDto pageDto) {
        String pageText;
        LemmaFinder lemmaFinder;
        try {
            pageText = pageDto.getContent();
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException exception) {
            log.error(exception.getMessage());
            return 0;
        }
        Map<String, Integer> lemmasWithFrequencies = lemmaFinder.collectLemmas(pageText);

        for (String lemmaKey : lemmasWithFrequencies.keySet()) {
            LemmaDto lemmaDto;
            if (lemmaCRUDService.isLemmaExist(lemmaKey)) {
                lemmaDto = lemmaCRUDService.getByLemma(lemmaKey);
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
