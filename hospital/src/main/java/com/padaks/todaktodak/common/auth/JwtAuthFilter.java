package com.padaks.todaktodak.common.auth;
import com.padaks.todaktodak.common.auth.JwtTokenprovider;
import com.padaks.todaktodak.doctor.domain.Doctor;
import com.padaks.todaktodak.doctor.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import com.padaks.todaktodak.common.domain.DelYN;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    private final JwtTokenprovider jwtTokenprovider;
    private final UserDetailsService userDetailsService;
    private final DoctorRepository doctorRepository;

    @Autowired
    public JwtAuthFilter(JwtTokenprovider jwtTokenprovider, UserDetailsService userDetailsService, DoctorRepository doctorRepository){
        this.jwtTokenprovider = jwtTokenprovider;
        this.userDetailsService = userDetailsService;
        this.doctorRepository = doctorRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        System.out.println("=====================resolve token from doFilterInternal=============");
        System.out.println(request);
        System.out.println("token : " + token);
        System.out.println("여기!!!");
        System.out.println(jwtTokenprovider);
        try{
            if ((token != null) && jwtTokenprovider.validateToken(token)){
                String email = jwtTokenprovider.getEmailFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (userDetails != null) {
                    Doctor doctor = doctorRepository.findByDoctorEmail(email).orElse(null);

                    if (doctor == null || doctor.getDelYN().equals(DelYN.Y)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("해당 계정은 삭제되었습니다.");
                        return;
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("서버 오류가 발생");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        System.out.println("=============bearer token from resolve token=========");
        System.out.println(bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
