package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.LemmaFinder;
import searchengine.dto.index.LemmaDto;
import searchengine.dto.index.SearchResponseElement;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.responseTemplates.ApiResponse;
import searchengine.responseTemplates.ErrorResponse;
import searchengine.responseTemplates.SuccessSearchResponse;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {
    private final SiteCRUDService siteCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final PageCRUDService pageCRUDService;
    private final IndexCRUDService indexCRUDService;
    private LemmaFinder lemmaFinder;
    public ApiResponse search(String query, String searchSiteUrl, Integer offset, Integer limit) {
        /* Определяем список сайтов для поиска */
        if (query.isEmpty()) {
            return new ErrorResponse("Задан пустой поисковый запрос");
        }
        List<Site> searchSiteList = new ArrayList<>();
        if (searchSiteUrl.isEmpty()) {
            searchSiteList.addAll(siteCRUDService.getAll().stream().map(SiteCRUDService::mapToEntity).toList());
        } else {
            searchSiteUrl = searchSiteUrl.endsWith("/") ? searchSiteUrl : searchSiteUrl + "/";
            Site searchSite = siteCRUDService.findByUrl(searchSiteUrl);
            if (searchSite == null) {
                return new ErrorResponse("Указанный сайт отсутствует в индексе.");
            }
            searchSiteList.add(searchSite);
        }
        System.out.println("Ищем на следующих сайтах: " + searchSiteList);

        /* Разбиваем поисковый запрос на леммы */
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException exception) {
            log.error("Не получилось инициализировать сервис LemmaFinder");
            return new ErrorResponse("Не получилось инициализировать сервис LemmaFinder");
        }
        Set<String> queryLemmaSet = lemmaFinder.collectLemmas(query).keySet();

        /* Общее число страниц, на которых ведется поиск */
        List<Page> pageList = new ArrayList<>();
        for (Site site : searchSiteList) {
            pageList.addAll(pageCRUDService.getPagesBySite(site.getId()));
        }
        int pagesAmount = pageList.size();

        /* Исключаем слишком популярные леммы */
        /* Коэффициент популярности, варьируется для обеспечения большей релевантности */
        float relevanceCoefficient = 1.0F; //0.4F;
        List<LemmaDto> lemmaDtoList = lemmaCRUDService.getLemmasListForSearching(queryLemmaSet.stream().toList(), (int) (pagesAmount * relevanceCoefficient));

        /* Нет информативных лемм, пустой ответ */
        if (lemmaDtoList.isEmpty()) {
            return new SuccessSearchResponse(true, 0, new ArrayList<>());
        }

        /* По самой редкой лемме ищем страницы, повторяем для каждой леммы, на каждой итерации оставляем только пересечение множеств. */
        List<Integer> pagesIds = indexCRUDService.getPageIdByLemma(lemmaDtoList.get(0).getId());
        for (LemmaDto ldto : lemmaDtoList) {
            pagesIds.retainAll(indexCRUDService.getPageIdByLemma(ldto.getId()));
        }

        /* По запросу страниц не найдено */
        if (pagesIds.isEmpty()) {
            return new SuccessSearchResponse(true, 0, new ArrayList<>());
        }
        List<Page> resultPages = pageCRUDService.getAllById(pagesIds);

        /* Формирование выходных данных, сниппеты */
        int resultsCount = resultPages.size();
        List<SearchResponseElement> foundData = new ArrayList<>();
        float maxRelevance = 0;
        for (Page p : resultPages) {
            String pageTitle, snippet;
            try {
                pageTitle = Jsoup.parse(p.getContent()).title();
                snippet = Jsoup.parse(p.getContent()).select("p:contains(" + query + ")").first().text();
                snippet = snippet.replaceAll(query, "<b>" + query + "</b>");
            }
            catch (NullPointerException e) {
                /* Ошибочно найденный вариант, исключаем */
                resultsCount--;
                continue;
            }

            /* По информативным леммам получаем абсолютную релевантность страницы */
            List<Integer> ids = lemmaDtoList.stream().map(LemmaDto::getId).toList();
            float relevance = indexCRUDService.getAbsRelevance(p.getId(), ids);
            maxRelevance = Math.max(relevance, maxRelevance);

            SearchResponseElement element = new SearchResponseElement(
                    p.getSite().getUrl(),
                    p.getSite().getName(),
                    p.getPath(),
                    pageTitle,
                    snippet,
                    relevance
            );
            foundData.add(element);
        }

        /* Переводим абсолютную релевантность в относительную */
        for (SearchResponseElement element : foundData) {
            element.setRelevance(element.getRelevance() / maxRelevance);
        }
        return new SuccessSearchResponse(true, resultsCount, foundData);
    }
}
