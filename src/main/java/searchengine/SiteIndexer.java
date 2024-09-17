package searchengine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.dto.index.PageDto;
import searchengine.services.PageCRUDServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
@Slf4j
public class SiteIndexer extends RecursiveTask<HashSet<String>> {

    private String rootUrl;
    private String currentUrl;
    private int rootSiteId;

    /** todo: autowired не сработал, прокидывать каждый раз сервис - трата ресурсов. как исправить? */
    private final PageCRUDServiceImpl pageCRUDService;
    private static HashSet<String> urlSet;

    public SiteIndexer(String url, String root, int rootSiteId, PageCRUDServiceImpl pageCRUDService) {
        rootUrl = root;
        currentUrl = url;
        this.rootSiteId = rootSiteId;
        if (urlSet == null) {
            urlSet = new HashSet<>();
            urlSet.add(root);
        }
        this.pageCRUDService = pageCRUDService;
    }

    @Override
    protected HashSet<String> compute() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Document doc = null;
        try {
            doc = Jsoup.connect(currentUrl).ignoreContentType(true).get();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return urlSet;
        }

        Elements links = doc.select("a[href]");
        Iterator<Element> iterator = links.iterator();
        List<SiteIndexer> walkers = new ArrayList<>();

        log.info("Current url is :" + currentUrl);
        log.info("Root url: " + rootUrl + " | Root site id: " + rootSiteId);
        pageCRUDService.create(new PageDto(
                rootSiteId,
                cutRootUrl(currentUrl),
                HttpStatus.OK.value(),
                doc.data()
        ));

        while (iterator.hasNext()) {
            Element element = iterator.next();
            String href = element.attr("abs:href").toLowerCase();
            href = checkSlash(href);

            if (href.contains(rootUrl) & !(urlSet.contains(href) | href.contains("#") )) {
                synchronized (urlSet) {
                    urlSet.add(href);
                }
                SiteIndexer walker = new SiteIndexer(href, rootUrl, rootSiteId, pageCRUDService);
                walker.fork();
                walkers.add(walker);
            }
        }

        for (SiteIndexer walker : walkers) {
            walker.join();
        }
        return urlSet;
    }

    private String cutRootUrl(String baseUrl) {
        return "/".concat(baseUrl.substring(rootUrl.length()));
    }

    private String checkSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl.concat("/");
    }
}
