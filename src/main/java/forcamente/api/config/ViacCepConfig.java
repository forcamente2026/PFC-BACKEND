package forcamente.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ViacCepConfig {
    @Bean
    public RestClient viaCepRestClient(@Value("${viacep.url}")String url) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica)
                .build();
    }

}
