package veterinaria.vargasvet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@org.springframework.scheduling.annotation.EnableAsync
public class VargasvetApplication {

	@jakarta.annotation.PostConstruct
	public void init() {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Lima"));
	}

	@org.springframework.context.annotation.Bean(name = "taskExecutor")
	public org.springframework.core.task.TaskExecutor taskExecutor() {
		org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(25);
		executor.setThreadNamePrefix("VargasVetAsync-");
		executor.initialize();
		return executor;
	}

	public static void main(String[] args) {
		SpringApplication.run(VargasvetApplication.class, args);
	}

}
