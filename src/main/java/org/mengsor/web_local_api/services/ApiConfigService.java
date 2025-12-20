package org.mengsor.web_local_api.services;

import org.mengsor.web_local_api.model.ApiConfig;

import java.util.List;

public interface ApiConfigService {
    ApiConfig find(String version, String apiName);
    ApiConfig findById(Long id);
    void save(ApiConfig apiConfig);
    void clear();
    List<ApiConfig> findAll();
    ApiConfig findByUrl(String url);
}
