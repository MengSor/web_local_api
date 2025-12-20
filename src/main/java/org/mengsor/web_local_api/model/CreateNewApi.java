package org.mengsor.web_local_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateNewApi {
    private Long id;
    private String name;
    private String baseUrl;
    private String protocol;
    private Date createdDate;
}

