package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.config.AppConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = AppConfiguration.class)
class EventSystemMongodbApplicationTests {

    @Test
    void contextLoads() {
    }

}
