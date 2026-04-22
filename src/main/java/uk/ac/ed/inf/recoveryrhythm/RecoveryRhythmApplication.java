package uk.ac.ed.inf.recoveryrhythm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecoveryRhythmApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecoveryRhythmApplication.class, args);
    }
}
