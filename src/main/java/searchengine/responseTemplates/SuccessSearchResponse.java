package searchengine.responseTemplates;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.dto.index.SearchResponseElement;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SuccessSearchResponse implements ApiResponse {
    private boolean result;
    private int count;
    private List<SearchResponseElement> data;
}
