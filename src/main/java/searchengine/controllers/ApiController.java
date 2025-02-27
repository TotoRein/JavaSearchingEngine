package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.index.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.responseTemplates.ApiResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
//
//    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
//        this.statisticsService = statisticsService;
//        this.indexingService = indexingService;
//    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(@RequestBody String url) {
        return indexingService.indexPage(url);
    }

    @GetMapping("/search")
    public ApiResponse search(@RequestParam String query,
                              @RequestParam(defaultValue = "") String site,
                              @RequestParam(defaultValue = "0") Integer offset,
                              @RequestParam(defaultValue = "20") Integer limit) {

        return searchService.search(query, site, offset, limit);
    }
}
