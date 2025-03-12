package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Query("UPDATE Site s SET s.status = 'FAILED', s.lastError = 'Индексация остановлена пользователем', s.statusTime = :statusTime WHERE s.status = 'INDEXING' AND s.id = :id")
    void setFailedStatusById(@Param("id") int id, @Param("statusTime") Date statusTime);

    @Modifying
    @Transactional
    @Query("UPDATE Site s SET s.status = 'INDEXED', s.statusTime = :statusTime WHERE s.id = :id")
    void setIndexedStatusById(@Param("id") int id, @Param("statusTime") Date statusTime);
    Site findByUrl(String url);

    @Modifying
    @Transactional
    @Query("UPDATE Site s SET s.statusTime = :statusTime WHERE s.id = :siteId")
    void updateStatusTime(@Param("siteId") int siteId, @Param("statusTime") Date statusTime);
}
