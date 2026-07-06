package com.creator.security;

import com.creator.model.User;
import com.creator.repository.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet filter that intercepts every incoming HTTP request and resolves the
 * tenant context from the X-User-Id header.
 *
 * Flow:
 * 1. Extract X-User-Id from the request header.
 * 2. Look up the User entity to find their agencyId and role.
 * 3. Populate TenantContext so all downstream code is automatically tenant-scoped.
 * 4. Clear TenantContext in the finally block to prevent thread-local leakage.
 *
 * Note: The H2 console and health-check paths are excluded from this filter
 * so they remain accessible without authentication.
 */
@Component
@Order(1)
public class TenantContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    public TenantContextFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        // Allow H2 console and health endpoints through without authentication
        if (path.startsWith("/h2-console")) {
            chain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(USER_ID_HEADER);

        if (userId == null || userId.isBlank()) {
            log.warn("Request to {} missing {} header", path, USER_ID_HEADER);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing X-User-Id header\"}");
            return;
        }

        try {
            // Look up user from the database to resolve their agency and role
            Optional<User> optionalUser = userRepository.findById(userId.trim());

            if (optionalUser.isEmpty()) {
                log.warn("Unknown user ID '{}' in {} header for request to {}", userId, USER_ID_HEADER, path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Unknown user\"}");
                return;
            }

            User user = optionalUser.get();

            // Populate the full tenant context — this is what scopes every downstream operation
            TenantContext.setCurrentUserId(user.getId());
            TenantContext.setCurrentAgencyId(user.getAgencyId());
            TenantContext.setCurrentUserRole(user.getRole());

            log.debug("TenantContext set — userId: {}, agencyId: {}, role: {}",
                    user.getId(), user.getAgencyId(), user.getRole());

            chain.doFilter(request, response);
        } finally {
            // Always clear to prevent thread-local leakage in pooled threads
            TenantContext.clear();
        }
    }
}

