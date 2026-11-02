package org.example.mesh2.common;

import org.example.mesh2.account.UserAccount;

public final class RequestContext {
    private static final ThreadLocal<UserAccount> CURRENT_USER = new ThreadLocal<>();

    private RequestContext() {}

    public static void setCurrentUser(UserAccount user) {
        CURRENT_USER.set(user);
    }

    public static UserAccount getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static UserAccount requireCurrentUser() {
        UserAccount user = CURRENT_USER.get();
        if (user == null) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        return user;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
