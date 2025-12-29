package org.mengsor.web_local_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mengsor.web_local_api.model.enums.SecurityMode;
import org.mengsor.web_local_api.model.enums.TokenUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettingCache {
    /* ================= BASIC ================= */
    private String username;
    private String password; // encrypted

    /* ================= SECURITY MODE ================= */
    private SecurityMode securityMode;

    /* ================= TOKEN / CLIENT ================= */
    private String clientId;
    private String clientSecret; // encrypted

    private TokenUnit tokenUnit;       // SECONDS | MINUTES | HOURS
    private Integer tokenDuration;
}

