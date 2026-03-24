package com.tongtu.docgen.system.model;

import lombok.Data;

import java.util.List;

@Data
public class UserContext {
    private Long userId;
    private String username;
    private String displayName;
    private List<String> roleCodes;
    private List<String> permissionCodes;
}


