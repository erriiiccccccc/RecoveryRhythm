package uk.ac.ed.inf.recoveryrhythm.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uk.ac.ed.inf.recoveryrhythm.service.S3StorageService;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3BucketInitializer implements CommandLineRunner {

    private final S3StorageService s3StorageService;

    @Override
    public void run(String... args) {
        int attempts = 0;
        while (attempts < 10) {
            attempts++;
            try {
                s3StorageService.ensureBucketExists();
                log.info("S3 bucket ready");
                return;
            } catch (Exception ex) {
                log.warn("S3 bucket init attempt {} failed: {}", attempts, ex.getMessage());
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.error("S3 bucket initialization failed after retries");
    }
}
