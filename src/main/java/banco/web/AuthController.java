package banco.web;

import banco.dto.SolicitudLogin;
import banco.model.Usuario;
import banco.repository.UsuarioRepository;
import banco.security.JwtService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Punto de entrada del login: valida credenciales con BCrypt y devuelve un JWT.
@RestController
@RequestMapping("/api")
public class AuthController {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody SolicitudLogin solicitud) {
        if (solicitud.usuario() == null || solicitud.password() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
        }
        Usuario usuario = usuarios.buscar(solicitud.usuario())
                .filter(Usuario::activo)
                .filter(u -> encoder.matches(solicitud.password(), u.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario o contraseña incorrectos."));
        return Map.of("token", jwt.generar(usuario.username(), usuario.rol()));
    }
}
