package kz.aitu.hrms.integration.exception;

import kz.aitu.hrms.common.exception.BusinessException;

import java.util.List;

public class SetupIncompleteException extends BusinessException {

    private final List<String> missingRequired;

    public SetupIncompleteException(List<String> missingRequired) {
        super("Setup incomplete — missing required keys: " + missingRequired);
        this.missingRequired = missingRequired;
    }

    public List<String> getMissingRequired() {
        return missingRequired;
    }
}
