package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findByLemma(String lemma);

    @Query(value = "SELECT * FROM `lemma` WHERE lemma in ?1 AND frequency < ?2 ORDER BY frequency ASC",
    nativeQuery = true)
    List<Lemma> findByLemmasAndFrequency(List<String> lemmas, int frequency);

    @Query(value = "SELECT COUNT(*) FROM `lemma` WHERE site_id = ?1",
    nativeQuery = true)
    int countLemmasBySite(int siteId);
}
