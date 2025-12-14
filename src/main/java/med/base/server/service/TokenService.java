package med.base.server.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import med.base.server.model.AuthAgent;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    public String getToken(AuthAgent authAgent) {

        return JWT.create().withAudience(String.valueOf(authAgent.getId()))
                .sign(Algorithm.HMAC256(authAgent.getPassword()));
    }
}
