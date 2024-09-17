package searchengine.services;

import org.springframework.http.ResponseEntity;

import java.util.Collection;

public interface CRUDService<T> {
    T getById(int id);
    Collection<T> getAll();
    T create(T item);
    ResponseEntity<?> update(T item);
    ResponseEntity<?> deleteById(int id);

}
