package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.index.IndexingResponse;

public interface IndexingService {
    IndexingResponse startIndexing();

    ResponseEntity<?> stopIndexing();

    IndexingResponse indexPage(String url);


}
