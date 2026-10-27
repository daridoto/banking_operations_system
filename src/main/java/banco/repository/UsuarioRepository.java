package banco.repository;

import banco.model.Usuario;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Usuario> convertirFila = (fila, numero) ->
            new Usuario(fila.getString("username"), fila.getString("password_hash"),
                    fila.getString("rol"), fila.getBoolean("activo"));

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Devuelve el usuario si existe; vacío si no. Quien llama decide qué hacer.
    public Optional<Usuario> buscar(String username) {
        return jdbc.query("SELECT * FROM usuarios WHERE username = ?", convertirFila, username)
                .stream().findFirst();
    }
}
