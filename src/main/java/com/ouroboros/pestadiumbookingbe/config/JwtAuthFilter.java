package com.ouroboros.pestadiumbookingbe.config;

import com.ouroboros.pestadiumbookingbe.exception.ForbiddenException;
import com.ouroboros.pestadiumbookingbe.model.Profile;
import com.ouroboros.pestadiumbookingbe.model.ProfileType;
import com.ouroboros.pestadiumbookingbe.repository.ProfileRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${supabase.jwt-secret}")
    private String jwtSecret;

    @Autowired
    private ProfileRepository profileRepository;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {  // If already authenticated, continue the filter chain
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = authHeader.substring("Bearer ".length());
        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes()); // Ensure the secret is properly encoded
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody();

            String userIdStr = claims.getSubject();
            UUID userId = UUID.fromString(userIdStr);
            String email = claims.get("email", String.class);

            ProfileType profileTypeRole = profileRepository.findById(userId)
                    .map(Profile::getType)
                    .orElseThrow(() -> new ForbiddenException("User not found"));
            String role = profileTypeRole.name().toUpperCase();

            Map<String, Object> userMetadata = claims.get("user_metadata", Map.class);
            String fullName = userMetadata != null ? (String) userMetadata.get("full_name") : "Unknown User";

            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            UserPrincipal userPrincipal = new UserPrincipal(
                    userId,
                    email,
                    fullName,
                    authorities
            );

            logger.info("Authenticated user: {}", userPrincipal.getUsername());
            logger.info("User role: {}", role);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
