package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.enums.Status;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Collection<Site>> findByStatus(Status status);
    @Modifying
    @Transactional
    @Query("UPDATE Site SET status = 'FAILED', last_error = 'Индексация остановлена пользователем', status_time =?2 WHERE status = 'INDEXING' AND id = ?1")
    void setFailedStatusById(int id, Date statusTime);

    @Modifying
    @Transactional
    @Query("UPDATE Site SET status = 'INDEXED', status_time = ?2 WHERE id = ?1")
    void setIndexedStatusById(int id, Date statusTime);
    Site findByUrl(String url);
}
