package org.mengsor.web_local_api.security.services;

import lombok.RequiredArgsConstructor;
import org.mengsor.web_local_api.model.SettingCache;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SettingCacheService cacheService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        SettingCache cache = cacheService.loadDecrypted();
        return org.springframework.security.core.userdetails.User
                .withUsername(cache.getUsername())
                .password(passwordEncoder.encode(cache.getPassword()))
                .roles("admin")
                .build();
    }
}