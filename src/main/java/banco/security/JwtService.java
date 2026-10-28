package banco.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Genera y verifica los JWT. Es el único que conoce la clave secreta.
@Service
public class JwtService {
    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${banco.jwt.secret}") String secreto,
                      @Value("${banco.jwt.expiracion-ms}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    // Crea un token firmado con el usuario, su rol y la fecha de caducidad dentro.
    public String generar(String usuario, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    // Comprueba la firma y la caducidad; devuelve los datos. Lanza si es inválido.
    public Claims verificar(String token) {
        return Jwts.parser().verifyWith(clave).build()
                .parseSignedClaims(token).getPayload();
    }
}
