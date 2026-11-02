package org.example.mesh2.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.mesh2.account.UserAccount;
import org.example.mesh2.account.UserRole;
import org.example.mesh2.common.ApiException;
import org.example.mesh2.common.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String TOKEN_HEADER = "X-Auth-Token";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequestContext.clear();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        boolean publicEndpoint = isPublic(path, request.getMethod());
        String token = request.getHeader(TOKEN_HEADER);

        if (token == null || token.isBlank()) {
            if (publicEndpoint) {
                return true;
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }

        UserAccount user = authService.resolveUser(token);
        RequestContext.setCurrentUser(user);

        if (requiresResponder(path, request.getMethod()) && user.getRole() == UserRole.CITIZEN) {
            RequestContext.clear();
            throw new ApiException(HttpStatus.FORBIDDEN, "Responder or admin access is required for this action.");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestContext.clear();
    }

    private boolean isPublic(String path, String method) {
        if (path.startsWith("/api/accounts/signup") || path.startsWith("/api/accounts/login")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/accounts/me") || path.startsWith("/api/sos/mine")) {
                return false;
            }
            return path.startsWith("/api/dashboard")
                    || path.startsWith("/api/incidents")
                    || path.startsWith("/api/sos")
                    || path.startsWith("/api/rescue")
                    || path.startsWith("/api/nodes")
                    || path.startsWith("/api/mesh")
                    || path.startsWith("/api/map");
        }
        // Allow LAN demo messaging without forcing login, but prefer authenticated sender when present.
        return "POST".equalsIgnoreCase(method) && path.startsWith("/api/mesh/messages");
    }

    private boolean requiresResponder(String path, String method) {
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method)) {
            return false;
        }
        return path.startsWith("/api/incidents")
                || path.matches("/api/sos/\\d+/(?:status|advance).*")
                || path.startsWith("/api/rescue/teams")
                || path.matches("/api/rescue/assignments/\\d+/status.*")
                || path.startsWith("/api/rescue/assign");
    }
}
