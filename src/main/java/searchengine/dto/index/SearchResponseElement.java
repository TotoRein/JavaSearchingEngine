package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class SearchResponseElement {
    private String siteUrl;
    private String siteName;
    private String pageUri;
    private String pageTitle;
    private String snippet;
    private float relevance;
}
