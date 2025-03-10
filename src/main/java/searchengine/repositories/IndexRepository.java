package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.Collection;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Collection<Index> findAllByPageId(int pageId);

    @Query(value = "SELECT page_id FROM `index` WHERE lemma_id = ?1",
    nativeQuery = true)
    List<Integer> findPageIdByLemmaId(int lemmaId);

    @Query(value = "SELECT SUM(`rank`) FROM `index` WHERE page_id = ?1 AND lemma_id IN ?2",
    nativeQuery = true)
    float getAbsRelevanceByPageAndLemmas(int pageId, List<Integer> lemmaIds);
}
