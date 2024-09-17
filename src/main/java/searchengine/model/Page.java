package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "page", indexes = @Index(columnList = "path"))
@Getter
@Setter
@AllArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    /** По заданию это поле должно быть TEXT,
     * но SQL не может создавать индекс по слишком длинным полям.
     * Задать длину индекса через jpa не получилось, изменил на VARCHAR(255) */
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
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
}
