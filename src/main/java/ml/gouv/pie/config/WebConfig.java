package ml.gouv.pie.config;

import lombok.RequiredArgsConstructor;
import ml.gouv.pie.service.StoredFileService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final StoredFileService storedFileService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storedFileService.uploadRoot().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
