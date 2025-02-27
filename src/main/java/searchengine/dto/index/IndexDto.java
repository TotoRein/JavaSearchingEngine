package searchengine.dto.index;

import lombok.*;

@Data
@AllArgsConstructor
@Getter
@Setter
public class IndexDto {
    int id;
    int pageId;
    int lemmaId;
    double rank;

    public IndexDto(int pageId, int lemmaId, double rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }
}
