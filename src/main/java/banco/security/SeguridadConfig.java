package banco.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// Configuración de seguridad: sustituye los valores por defecto de Spring Security.
// Solo se aplica cuando hay capa web (servlet); sin ella no existe HttpSecurity.
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SeguridadConfig {

    // Pieza 1: el codificador. Sabe calcular un hash BCrypt y comparar una
    // contraseña contra un hash. No guarda nada; solo hace el cálculo.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Los usuarios ya no viven aquí: los lee UsuarioDetailsService desde la
    // tabla usuarios de H2. Spring une ese servicio con el encoder de arriba
    // para autenticar, sin que haya ninguna contraseña en el código.

    // Pieza 3: las reglas. Resolver incidencias exige rol SUPERVISOR; el resto
    // de rutas, cualquier usuario autenticado. Se usa HTTP Basic.
    // CSRF se desactiva porque el cliente es de escritorio (no es un navegador
    // con cookies), que es el escenario donde ese ataque tiene sentido.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/incidencias/*/resolucion").hasRole("SUPERVISOR")
                        .anyRequest().authenticated())
                .httpBasic(basic -> {});
        return http.build();
    }
}
