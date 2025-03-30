package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.index.LemmaDto;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LemmaCRUDService implements CRUDService<LemmaDto> {

    private final LemmaRepository lemmaRepository;
    private final SiteCRUDService siteCRUDService;

    @Override
    public LemmaDto getById(int id) {
        try {
            Lemma lemma = lemmaRepository.findById(id).orElseThrow(ChangeSetPersister.NotFoundException::new);
            return mapToDto(lemma);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Collection<LemmaDto> getAll() {
        return lemmaRepository.findAll().stream().map(LemmaCRUDService::mapToDto).toList();
    }

    @Override
    public LemmaDto create(LemmaDto lemmaDto) {
        Site site = SiteCRUDService.mapToEntity(siteCRUDService.getById(lemmaDto.getSiteId()));
        Lemma lemma = new Lemma(site, lemmaDto.getLemma(), lemmaDto.getFrequency());
        lemma = lemmaRepository.saveAndFlush(lemma);
        return mapToDto(lemma);
    }

    @Override
    public ResponseEntity<?> update(LemmaDto lemmaDto) {
        Lemma lemma = mapToEntity(lemmaDto);
        Site site = SiteCRUDService.mapToEntity(siteCRUDService.getById(lemmaDto.getSiteId()));
        lemma.setSite(site);
        lemma = lemmaRepository.save(lemma);
        return ResponseEntity.ok(mapToDto(lemma));
    }

    @Override
    public ResponseEntity<?> deleteById(int id) {
        lemmaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public static LemmaDto mapToDto(Lemma lemma) {
        return new LemmaDto(lemma.getId(), lemma.getSite().getId(), lemma.getLemma(), lemma.getFrequency());
    }

    public static Lemma mapToEntity(LemmaDto lemmaDto) {
        return new Lemma(lemmaDto.getId(), null, lemmaDto.getLemma(), lemmaDto.getFrequency());
    }

    public LemmaDto getByLemma(String lemma) {
        Optional<Lemma> lemmaEntity = lemmaRepository.findByLemma(lemma);
        return lemmaEntity.map(LemmaCRUDService::mapToDto).orElse(null);
    }

    public List<LemmaDto> getLemmasListForSearching(List<String> lemmasList, int frequency) {
        return lemmaRepository.findByLemmasAndFrequency(lemmasList, frequency).stream().map(LemmaCRUDService::mapToDto).toList();
    }

    public int getLemmasAmountBySiteId(int siteId) {
        return lemmaRepository.countLemmasBySite(siteId);
    }
}
