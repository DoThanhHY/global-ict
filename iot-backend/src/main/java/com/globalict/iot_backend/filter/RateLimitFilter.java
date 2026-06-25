package com.globalict.iot_backend.filter;

import com.globalict.iot_backend.config.RateLimitingConfig;
import com.globalict.iot_backend.config.RateLimitingConfig.BucketRegistry;
import com.globalict.iot_backend.config.RateLimitingConfig.Tier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final BucketRegistry bucketRegistry;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        Tier tier = RateLimitingConfig.TIER_RESOLVER.resolve(method, path);
        String key = resolveKey(request);

        boolean consumed = bucketRegistry.resolve(key, tier).tryConsume(1);

        if (!consumed) {
            String message = "Rate limit exceeded for " + tier.name().toLowerCase()
                    + " endpoint. Try again later.";
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            String json = "{\"error\":\"Too many requests\",\"message\":\""
                    + message.replace("\"", "\\\"")
                    + "\",\"retryAfterSeconds\":60}";
            response.getWriter().write(json);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            return auth.getName();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }
}
