package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "`index`")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @OneToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(nullable = false, columnDefinition = "FLOAT")
    private double rank;

    public Index(Page page, Lemma lemma, double rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }
}
