package banco.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Configuración de seguridad: sustituye los valores por defecto de Spring Security.
// Solo se aplica cuando hay capa web (servlet); sin ella no existe HttpSecurity.
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SeguridadConfig {

    // El codificador BCrypt está en CodificadorConfig (lo usa también el login).
    // Los usuarios los lee UsuarioDetailsService desde la tabla usuarios de H2.

    // Las reglas. /api/login es abierto (es donde se obtiene el token);
    // resolver incidencias exige rol SUPERVISOR; el resto, usuario autenticado.
    // Sin estado (stateless): no hay sesión de servidor, cada petición trae su token.
    // CSRF se desactiva porque no usamos cookies de sesión (el token va en la cabecera).
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                        // Spring reenvía los errores (403, 404...) a /error en una segunda
                        // pasada sin token; si /error exigiera login, todo acabaría en 401.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/incidencias/*/resolucion").hasRole("SUPERVISOR")
                        .anyRequest().authenticated())
                // Sin mecanismo que "reto" (como Basic), una petición sin token
                // cae por defecto en 403; forzamos el 401 correcto (no autenticado).
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (peticion, respuesta, fallo) -> respuesta.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
