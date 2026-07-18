package fr.fezlight.eventsystem.config.rabbitmq;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Optional;
import java.util.function.Supplier;

public class QueueNameResolver {
    private final ObjectProvider<String> mainQueueName;
    private final ObjectProvider<Supplier<String>> mainQueueNameLegacy;

    private final ObjectProvider<String> workerQueueName;
    private final ObjectProvider<Supplier<String>> workerQueueNameLegacy;

    public QueueNameResolver(@Qualifier("defaultMainQueueName") ObjectProvider<String> mainQueueName,
                             @Qualifier("defaultMainQueueNaming") ObjectProvider<Supplier<String>> mainQueueNameLegacy,
                             @Qualifier("defaultWorkerQueueName") ObjectProvider<String> workerQueueName,
                             @Qualifier("defaultWorkerQueueNaming") ObjectProvider<Supplier<String>> workerQueueNameLegacy) {
        this.mainQueueName = mainQueueName;
        this.mainQueueNameLegacy = mainQueueNameLegacy;
        this.workerQueueName = workerQueueName;
        this.workerQueueNameLegacy = workerQueueNameLegacy;
    }

    public String getMainQueueName() {
        return Optional.ofNullable(mainQueueNameLegacy.getIfAvailable())
                .map(Supplier::get)
                .or(() -> Optional.ofNullable(mainQueueName.getIfAvailable()))
                .orElseThrow(() -> new IllegalStateException("No main queue naming bean available"));
    }

    public String getWorkerQueueName() {
        return Optional.ofNullable(workerQueueNameLegacy.getIfAvailable())
                .map(Supplier::get)
                .or(() -> Optional.ofNullable(workerQueueName.getIfAvailable()))
                .orElseThrow(() -> new IllegalStateException("No worker queue naming bean available"));
    }
}
