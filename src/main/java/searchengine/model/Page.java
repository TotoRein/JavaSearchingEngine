package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`page`", indexes = {@Index(columnList = "path"), @Index(columnList = "site_id, path", unique = true)})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    /** По заданию это поле должно быть TEXT,
     * но SQL не может создавать индекс по слишком длинным полям.
     * Задать длину индекса через jpa не получилось, изменил на VARCHAR(255)
     * Иногда возникают ошибки кодировки, в бд поменял на utf8bm4, для создания индекса сокращаю до 180 */
    @Column(nullable = false, columnDefinition = "VARCHAR(180)")
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private  String content;

    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<searchengine.model.Index> indexList = new ArrayList<>();

    @Override
    public String toString() {
        return "id=" + id + "\n" +
                "site=" + site.getUrl() + "\n" +
                "path=" + path;
    }
}
