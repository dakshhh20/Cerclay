package com.mittiandmore.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Keeps customer and admin SecurityContexts in the same HTTP session under
 * different session attributes. This allows both applications to run on the
 * same localhost origin/port without one login replacing the other.
 *
 * The admin frontend marks its requests with X-Client-Type: admin.
 */
public class DualClientSecurityContextRepository implements SecurityContextRepository {

    public static final String ADMIN_HEADER = "X-Client-Type";
    public static final String ADMIN_VALUE = "admin";

    private final HttpSessionSecurityContextRepository customerRepository = new HttpSessionSecurityContextRepository();
    private final HttpSessionSecurityContextRepository adminRepository = new HttpSessionSecurityContextRepository();

    public DualClientSecurityContextRepository() {
        customerRepository.setSpringSecurityContextKey("CERCLAY_CUSTOMER_SECURITY_CONTEXT");
        adminRepository.setSpringSecurityContextKey("CERCLAY_ADMIN_SECURITY_CONTEXT");
    }

    private SecurityContextRepository repository(HttpServletRequest request) {
        return ADMIN_VALUE.equalsIgnoreCase(request.getHeader(ADMIN_HEADER)) ? adminRepository : customerRepository;
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        return repository(requestResponseHolder.getRequest()).loadContext(requestResponseHolder);
    }

    @Override
    public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
        return repository(request).loadDeferredContext(request);
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return repository(request).containsContext(request);
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        repository(request).saveContext(context, request, response);
    }

    public SecurityContextRepository customerRepository() {
        return customerRepository;
    }

    public SecurityContextRepository adminRepository() {
        return adminRepository;
    }
}
