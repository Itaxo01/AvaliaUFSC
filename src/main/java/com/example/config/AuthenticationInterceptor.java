package com.example.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.service.SessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

	 @Autowired
	 private SessionService sessionService;

    // URLs accessible to non-authenticated users
    private static final String[] PUBLIC_URLS = {
        "/", "/login/**", "/register/**", "/css/**", "/js/**", "/images/**", "/error", "/forgot-password/**", "/reset-password/**"
    };

    // URLs that require authentication but not admin
    private static final String[] USER_URLS = {
        "/index", "/user", "/user/**", "/class", "/class/**", "/api/me", "/logout"
    };

    // URLs that require admin privileges
    private static final String[] ADMIN_URLS = {
        "/admin", "/admin/**", "/api/admin/**", "/h2-console", "/h2-console/**"
    };
	
	 // Development-only URLs (super admin access)
    private static final String[] DEV_ADMIN_URLS = {
        "/h2-console", "/h2-console/**"
    };
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                           @NonNull Object handler) throws Exception {
        
        String requestURI = request.getRequestURI();
        boolean isAuthenticated = sessionService.verifySession(request);
        boolean isAdmin = isAuthenticated && sessionService.currentUserIsAdmin(request);

        // Check if URL is public
        if (isPublicUrl(requestURI)) {
            return true;
        }

		  if (isDevAdminUrl(requestURI)) {
            // Only allow in development profile AND super admin
            if (!isDevelopmentMode()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return false;
            }
            if (!isAuthenticated || !isSuperAdmin(request)) {
                response.sendRedirect("/login?error=superAdminRequired");
                return false;
            }
            return true;
        }

        // Check if URL requires admin
        if (isAdminUrl(requestURI)) {
            if (!isAuthenticated) {
                response.sendRedirect("/login?error=notAuthenticated");
                return false;
            }
            if (!isAdmin) {
                response.sendRedirect("/index?error=accessDenied");
                return false;
            }
            return true;
        }

        // Check if URL requires user authentication
        if (isUserUrl(requestURI)) {
            if (!isAuthenticated) {
                response.sendRedirect("/login?error=notAuthenticated");
                return false;
            }
            return true;
        }

        // Default: require authentication (Vai ativar na maioria dos endpoints da api)
		  if (!isAuthenticated) {
					response.sendRedirect("/login?error=notAuthenticated");
					return false;
			}

        return true;
    }

    private boolean isPublicUrl(String uri) {
        return matchesPattern(uri, PUBLIC_URLS);
    }

    private boolean isUserUrl(String uri) {
        return matchesPattern(uri, USER_URLS);
    }

    private boolean isAdminUrl(String uri) {
        return matchesPattern(uri, ADMIN_URLS);
    }
	 private boolean isDevAdminUrl(String uri) {
        return matchesPattern(uri, DEV_ADMIN_URLS);
    }

    private boolean isDevelopmentMode() {
        // Check if running in development profile
        String[] activeProfiles = System.getProperty("spring.profiles.active", "").split(",");
        return Arrays.asList(activeProfiles).contains("dev") || 
               Arrays.asList(activeProfiles).contains("development");
    }

    private boolean isSuperAdmin(HttpServletRequest request) {
        String user_email = sessionService.getCurrentUser(request);
        // Only specific emails can access H2 console
        return user_email != null && 
               ("kauanfank@gmail.com".equals(user_email));
    }

    private boolean matchesPattern(String uri, String[] patterns) {
        for (String pattern : patterns) {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (uri.startsWith(prefix)) {
                    return true;
                }
            } else if (pattern.endsWith("**")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                if (uri.startsWith(prefix)) {
                    return true;
                }
            } else if (uri.equals(pattern)) {
                return true;
            }
        }
        return false;
    }
}