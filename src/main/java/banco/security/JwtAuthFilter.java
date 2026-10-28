package banco.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Se ejecuta en cada petición: si viene un token Bearer válido, autentica al usuario.
// No consulta la base de datos; le basta con verificar la firma del token.
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String cabecera = peticion.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            String token = cabecera.substring("Bearer ".length());
            try {
                Claims datos = jwt.verificar(token);
                String usuario = datos.getSubject();
                String rol = datos.get("rol", String.class);
                var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (JwtException e) {
                // Token inválido o caducado: no autenticamos; acabará en 401.
                SecurityContextHolder.clearContext();
            }
        }
        cadena.doFilter(peticion, respuesta);
    }
}
