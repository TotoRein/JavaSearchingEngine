package searchengine.dto.index;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Data
public class LemmaDto {
    private int id;
    private int siteId;
    private String lemma;
    private int frequency;

    public LemmaDto(int siteId, String lemma, int frequency) {
        this.siteId = siteId;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public String toString() {
        return "LemmaDto{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}
