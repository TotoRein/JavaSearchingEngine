package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
@AllArgsConstructor
public class PageDto {
    private int id;
    private int siteId;
    private String path;
    private int code;
    private String content;

    public PageDto(int siteId, String path, int code, String content) {
        this.siteId = siteId;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}
