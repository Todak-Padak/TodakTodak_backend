package com.padaks.todaktodak.common.config;

import com.padaks.todaktodak.common.auth.JwtTokenprovider;
import com.padaks.todaktodak.doctor.domain.Doctor;
import com.padaks.todaktodak.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import com.padaks.todaktodak.common.domain.DelYN;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

//    private final com.padaks.todaktodak.common.config.JwtTokenprovider jwtTokenProvider;
    
    private final JwtTokenprovider jwtTokenprovider;
    private final UserDetailsService userDetailsService;
    private final DoctorRepository doctorRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (userDetails != null) {
                Doctor doctor = doctorRepository.findByDoctorEmail(email)
                        .orElse(null);

                if (doctor == null || doctor.getDoctorEmail() == null || doctor.getDelYN().equals(DelYN.Y)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("해당 계정은 삭제되었습니다.");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
