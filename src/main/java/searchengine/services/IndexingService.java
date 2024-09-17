package searchengine.services;

import searchengine.dto.index.IndexingResponse;

public interface IndexingService {
    IndexingResponse startIndexing();
}
