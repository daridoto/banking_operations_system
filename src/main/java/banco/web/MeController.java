package banco.web;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// "Quién soy": devuelve el usuario autenticado y su rol, para que el cliente
// adapte la interfaz. No es una operación bancaria, por eso va en su propio controller.
@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public Map<String, String> me(Authentication autenticacion) {
        // Spring guarda el rol como "ROLE_EMPLEADO"; le quitamos el prefijo.
        String rol = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
        return Map.of("usuario", autenticacion.getName(), "rol", rol);
    }
}
