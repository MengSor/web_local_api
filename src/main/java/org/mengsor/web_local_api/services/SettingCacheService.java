package org.mengsor.web_local_api.services;

import org.mengsor.web_local_api.model.SettingCache;

public interface SettingCacheService {
    void save(SettingCache settingCache);
    SettingCache load();
    SettingCache loadDecrypted();
}

