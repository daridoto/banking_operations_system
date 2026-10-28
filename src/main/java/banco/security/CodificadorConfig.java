package banco.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// El codificador de contraseñas no depende de la capa web (lo usa el login y
// podría usarlo cualquier servicio), así que vive en su propia configuración.
@Configuration
public class CodificadorConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
