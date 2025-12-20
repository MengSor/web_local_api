package org.mengsor.web_local_api.services;

import org.mengsor.web_local_api.model.CreateNewApi;

import java.util.List;

public interface CreateNewApiService {
    List<CreateNewApi> getAll();
    void save(CreateNewApi api);
    List<CreateNewApi> loadAll();
    Long findLastId();
    void delete(Long id);
    void clear();
}
