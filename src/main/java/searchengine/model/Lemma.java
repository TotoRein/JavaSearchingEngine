package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`lemma`")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    public Lemma(Site site, String lemma, int frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    public Lemma(int id, Site site, String lemma, int frequency) {
        this.id = id;
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Index> indexList = new ArrayList<>();
}
