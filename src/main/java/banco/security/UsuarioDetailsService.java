package banco.security;

import banco.repository.UsuarioRepository;
import banco.model.Usuario;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Puente entre nuestra tabla usuarios y Spring Security.
// Spring llama a loadUserByUsername cuando llega un login; aquí buscamos en H2.
@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;

    public UsuarioDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario usuario = usuarios.buscar(username)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario " + username + "."));

        // Convertimos nuestra fila en el UserDetails que Spring entiende.
        // La contraseña es el hash; Spring compara contra él, no lo descifra.
        return User.withUsername(usuario.username())
                .password(usuario.passwordHash())
                .roles(usuario.rol())
                .disabled(!usuario.activo())
                .build();
    }
}
