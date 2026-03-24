package com.tongtu.docgen.system.model;

import lombok.Data;

import java.util.List;

@Data
public class AuthProfile {
    private Long userId;
    private String username;
    private String displayName;
    private String status;
    private List<String> roleCodes;
    private List<String> permissionCodes;
}


