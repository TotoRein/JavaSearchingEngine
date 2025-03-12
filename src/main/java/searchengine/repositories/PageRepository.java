package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "SELECT id FROM page WHERE site_id = ?1 AND `path` = ?2",
            nativeQuery = true)
    Optional<Integer> checkPage(int siteId, String path);
    Optional<Page> findBySiteIdAndPath(int siteId, String path);

    List<Page> findBySiteId(int siteId);
}
