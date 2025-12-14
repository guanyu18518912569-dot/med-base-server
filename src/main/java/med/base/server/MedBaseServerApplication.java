package med.base.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("med.base.server.mapper")
public class MedBaseServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedBaseServerApplication.class, args);
    }

}
