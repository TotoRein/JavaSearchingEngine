package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.enums.Status;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@Getter
@Setter
public class SiteDto {
    private int id;
    private Status status;
    private Date statusTime;
    private String lastError;
    private String url;
    private String name;
    private List<PageDto> pages;

    public SiteDto(String url) {
        this.url = url;
    }
}
