package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndexingResponse {
    private boolean result;
    private String message;
}
