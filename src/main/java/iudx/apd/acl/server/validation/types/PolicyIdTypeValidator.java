package iudx.apd.acl.server.validation.types;


import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.validation.Validator;

import iudx.apd.acl.server.validation.exceptions.DxRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.apd.acl.server.apiserver.util.Constants.POLICY_ID_MAX_LENGTH;
import static iudx.apd.acl.server.apiserver.util.Constants.POLICY_ID_PATTERN;
import static iudx.apd.acl.server.common.ResponseUrn.INVALID_ID_VALUE_URN;

public final class PolicyIdTypeValidator implements Validator {

    private static final Logger LOGGER = LogManager.getLogger(PolicyIdTypeValidator.class);

    private final String value;
    private final boolean required;

    public PolicyIdTypeValidator(final String value, final boolean required) {
        this.value = value;
        this.required = required;
    }

    public boolean isValidPolicyId(final String value) {
        return POLICY_ID_PATTERN.matcher(value).matches();
    }

    @Override
    public boolean isValid() {
        LOGGER.debug("value : " + value + "required : " + required);
        if (required && (value == null || value.isBlank())) {
            LOGGER.error("Validation error : null or blank value for required mandatory field");
            throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage());
        } else {
            if (value == null) {
                return true;
            }
            if (value.isBlank()) {
                LOGGER.error("Validation error :  blank value for passed");
                throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
            }
        }
        if (value.length() > POLICY_ID_MAX_LENGTH) {
            LOGGER.error("Validation error : Value exceed max character limit.");
            throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
        }
        if (!isValidPolicyId(value)) {
            LOGGER.error("Validation error : Invalid id.");
            throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
        }
        return true;
    }

    @Override
    public int failureCode() {
        return HttpStatusCode.BAD_REQUEST.getValue();
    }

    @Override
    public String failureMessage() {
        return INVALID_ID_VALUE_URN.getMessage();
    }
}
