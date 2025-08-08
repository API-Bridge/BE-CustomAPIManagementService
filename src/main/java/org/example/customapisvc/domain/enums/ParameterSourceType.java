package org.example.customapisvc.domain.enums;

import lombok.Getter;

// 외부API가 필요한 파라미터 값의 출처
@Getter
public enum ParameterSourceType {
    INITIAL_REQUEST("INITIAL_REQUEST"),
    STEP_OUTPUT("STEP_OUTPUT"), 
    STATIC_VALUE("STATIC_VALUE");

    private final String value;

    ParameterSourceType(String value) {
        this.value = value;
    }

}