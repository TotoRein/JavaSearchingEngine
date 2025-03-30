package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.dto.index.IndexDto;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexCRUDService implements CRUDService<IndexDto> {
    private final IndexRepository indexRepository;
    private final LemmaCRUDService lemmaCRUDService;
    private final PageCRUDService pageCRUDService;
    private final JdbcTemplate jdbcTemplate;
    @Override
    public IndexDto getById(int id) {
        try {
            Index index = indexRepository.findById(id).orElseThrow(ChangeSetPersister.NotFoundException::new);
            return mapToDto(index);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Collection<IndexDto> getAll() {
        return indexRepository.findAll().stream().map(IndexCRUDService::mapToDto).toList();
    }

    @Override
    public IndexDto create(IndexDto dto) {
        Lemma lemma = LemmaCRUDService.mapToEntity(lemmaCRUDService.getById(dto.getLemmaId()));
        Page page = PageCRUDService.mapToEntity(pageCRUDService.getById(dto.getPageId()));
        Index index = new Index(dto.getId(), page, lemma, dto.getRank());
        index = indexRepository.saveAndFlush(index);
        return mapToDto(index);
    }

    @Override
    public ResponseEntity<?> update(IndexDto dto) {
        Lemma lemma = LemmaCRUDService.mapToEntity(lemmaCRUDService.getById(dto.getLemmaId()));
        Page page = PageCRUDService.mapToEntity(pageCRUDService.getById(dto.getPageId()));
        Index index = IndexCRUDService.mapToEntity(dto);
        index.setPage(page);
        index.setLemma(lemma);
        index = indexRepository.saveAndFlush(index);
        return ResponseEntity.ok(mapToDto(index));
    }

    @Override
    public ResponseEntity<?> deleteById(int id) {
        indexRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public static IndexDto mapToDto(Index indexEntity) {
        return new IndexDto(indexEntity.getId(),
                indexEntity.getPage().getId(),
                indexEntity.getLemma().getId(),
                indexEntity.getRank());
    }

    public static Index mapToEntity(IndexDto dto) {
        return new Index(dto.getId(), null, null, dto.getRank());
    }

    public Collection<IndexDto> getIndexesByPageId(int pageId) {
        return indexRepository.findAllByPageId(pageId).stream().map(IndexCRUDService::mapToDto).toList();
    }

    public List<Integer> getPageIdByLemma(int lemmaId) {
        return indexRepository.findPageIdByLemmaId(lemmaId);
    }

    public float getAbsRelevance(int pageId, List<Integer> lemmaIds) {
        return indexRepository.getAbsRelevanceByPageAndLemmas(pageId, lemmaIds);
    }

    public void createAll(List<IndexDto> list) {
        jdbcTemplate.batchUpdate("INSERT INTO `index` (page_id, lemma_id, `rank`) VALUES(?, ?, ?)",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) {
                    try {
                        ps.setInt(1, list.get(i).getPageId());
                        ps.setInt(2, list.get(i).getLemmaId());
                        ps.setDouble(3, list.get(i).getRank());
                    } catch (SQLException e) {
                        log.error(e.getMessage());
                    }
                }

                @Override
                public int getBatchSize() {
                    return list.size();
                }
            });
    }
}
