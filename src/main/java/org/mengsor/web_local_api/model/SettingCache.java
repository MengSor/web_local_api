package org.mengsor.web_local_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mengsor.web_local_api.model.enums.SecurityMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettingCache {
    private String username;
    private String password; // encrypted
    private SecurityMode securityMode;
}

