package kz.aitu.hrms.common.jwt;

/**
 * Single source of truth for JWT claim names. The token issuer (user-service)
 * and every validator in hrms-common reference these constants so a name change
 * here propagates to every consumer at compile time.
 */
public final class JwtClaims {

    public static final String EMAIL = "email";
    public static final String ROLE = "role";
    public static final String PERMISSIONS = "permissions";
    public static final String EMPLOYEE_ID = "employeeId";
    public static final String TYPE = "type";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private JwtClaims() {}
}