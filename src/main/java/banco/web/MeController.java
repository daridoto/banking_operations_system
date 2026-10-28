package banco.web;

import banco.model.Usuario;
import banco.repository.UsuarioRepository;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// "Quién soy": devuelve el usuario autenticado, su nombre y su rol, para que el
// cliente adapte la interfaz. No es una operación bancaria, por eso va en su propio controller.
@RestController
@RequestMapping("/api")
public class MeController {
    private final UsuarioRepository usuarios;

    public MeController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication autenticacion) {
        // Spring guarda el rol como "ROLE_EMPLEADO"; le quitamos el prefijo.
        String rol = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
        // El token solo lleva usuario y rol; el nombre se lee de la base. Si no se
        // encuentra (no debería), se muestra el usuario para no dejar la cabecera vacía.
        String nombre = usuarios.buscar(autenticacion.getName())
                .map(Usuario::nombreCompleto)
                .orElse(autenticacion.getName());
        return Map.of("usuario", autenticacion.getName(), "nombre", nombre, "rol", rol);
    }
}
