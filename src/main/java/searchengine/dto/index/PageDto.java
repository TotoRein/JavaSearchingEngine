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

    public PageDto(int siteId, String path) {
        this.siteId = siteId;
        this.path = path;
    }

    @Override
    public String toString() {
        return "PageDto{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content length='" + content.length() + '\'' +
                '}';
    }
}
