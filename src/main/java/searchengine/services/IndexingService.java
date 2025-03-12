package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.index.IndexingResponse;

import java.util.Map;

public interface IndexingService {
    IndexingResponse startIndexing();

    ResponseEntity<?> stopIndexing();

    IndexingResponse indexPage(String url);


}
