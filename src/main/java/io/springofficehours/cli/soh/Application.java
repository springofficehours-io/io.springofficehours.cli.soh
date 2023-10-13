package io.springofficehours.cli.soh;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.shell.Availability;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

@EnableConfigurationProperties(ConfigProps.class)
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

@Component
record CustomPromptProvider() implements PromptProvider {

	@Override
	public AttributedString getPrompt() {
		return new AttributedString("spring-office-hours:>", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
	}

}

@ShellComponent
class ImageCommands implements HealthIndicator {

	ImageServiceClient imageServiceClient;

	public ImageCommands(ImageServiceClient imageServiceClient) {
		this.imageServiceClient = imageServiceClient;
	}

	@Reflective
	public Availability healthyService() {
		return health().getStatus().equals(Status.UP) ? Availability.available()
				: Availability.unavailable("Image Service Not Healthy");
	}

	@ShellMethod("get blank template file")
	@ShellMethodAvailability("healthyService")
	public String template(@ShellOption(defaultValue = "./template.png") String path) {
		try {
			byte[] image = imageServiceClient.getTemplateImage();
			Files.write(new File(path).toPath(), image);
		}
		catch (IOException ioe) {
			return "There was a problem: " + ioe.getMessage();
		}
		return path;
	}

	@ShellMethod("get streamyard thumbnail")
	@ShellMethodAvailability("healthyService")
	public String streamyard(@ShellOption(defaultValue = "./streamyard.png") String path) {
		try {
			byte[] image = imageServiceClient.getStreamyardImage();
			Files.write(new File(path).toPath(), image);
		}
		catch (IOException ioe) {
			return "There was a problem: " + ioe.getMessage();
		}
		return path;
	}

	@ShellMethod("Create Streamyard episode")
	@ShellMethodAvailability("healthyService")
	public String schedule(@ShellOption(defaultValue = "0000") String episodeNumber,
			@ShellOption(defaultValue = "#TODO") String title,
			@ShellOption(defaultValue = "dashaunc@vmware.com") String email) {
		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
			BrowserContext context = browser.newContext();
			Page page = context.newPage();
			page.navigate("https://streamyard.com/login");
			page.getByTestId("LoginEmailInput").click();
			page.getByTestId("LoginEmailInput").fill("dashaunc@vmware.com");
			page.getByTestId("LoginEmailSubmit").click();
			// The page changes to a code input page
			page.getByTestId("LoginCodeInput").click();
			// Ask for the code sent via email
			page.getByTestId("LoginCodeInput").fill("573450");
			page.getByTestId("LoginCodeSubmit").click();
			page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create")).click();
			page.getByRole(AriaRole.BUTTON,
					new Page.GetByRoleOptions().setName("Live stream Use the studio or a pre-recorded video"))
				.click();
			page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("SpringDeveloper")).check();
			page.getByLabel("Title").click();
			page.getByLabel("Title").fill("Spring Office Hours - Episode 1000 - Show Title");
			page.getByPlaceholder("Say something about this broadcast").click();
			page.getByPlaceholder("Say something about this broadcast").fill("DESCRIPTION GOES HERE");
			page.getByTestId("checkbox-checkbox_i3rSQ4y2A2Os").check();
			page.getByText("Upload thumbnail").click();
			page.getByLabel("Upload thumbnail").setInputFiles(Paths.get("streamyard-thumbnail_talking_heads.png"));
			page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply")).click();
			page.getByRole(AriaRole.TEXTBOX).nth(3).click();
			page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("26")).click();
			page.locator("#select_vBzKPBEOQg41").selectOption("2");
			page.getByRole(AriaRole.COMBOBOX).nth(2).selectOption("30");
			page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create broadcast")).click();
		}
		return "done";
	}

	@ShellMethod("Spring Office Hours Episode Image")
	@ShellMethodAvailability("healthyService")
	public String show(@ShellOption(defaultValue = "0000") String episodeNumber,
			@ShellOption(defaultValue = "#TODO") String title, @ShellOption(defaultValue = "2023-06-09") String date,
			@ShellOption(defaultValue = "#TODO") String youTubeId,
			@ShellOption(defaultValue = "12:30:00-07:00") String time,
			@ShellOption(defaultValue = "./content/tv/spring-office-hours") String baseDir) {
		String path = "%s/%s".formatted(baseDir, episodeNumber);
		String imagesPath = "%s/images".formatted(path);
		File file = new File(path);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				return "Couldn't create directory: " + path;
			}
		}
		File imagesDir = new File(imagesPath);
		if (!imagesDir.exists()) {
			if (!imagesDir.mkdirs()) {
				return "Couldn't create directory: " + imagesPath;
			}
		}
		try {
			byte[] image = imageServiceClient
				.getCustomSpringOfficeHours("Episode: %s - %s".formatted(Integer.valueOf(episodeNumber), title));
			Files.write(new File("%s/images/%s.png".formatted(path, episodeNumber)).toPath(), image);
			writeStringToFile(showTemplate.formatted(date, time, date, date, Integer.valueOf(episodeNumber), title,
					episodeNumber, youTubeId), new File("%s/index.md".formatted(path)));
		}
		catch (IOException ioe) {
			return "There was a problem: " + ioe.getMessage();
		}
		return path;
	}

	@Override
	public Health health() {
		int errorCode = check();
		if (errorCode != 0) {
			return Health.down().withDetail("Error Code", errorCode).build();
		}
		return Health.up().build();
	}

	private int check() {
		if (!imageServiceClient.serviceHealth().contains("UP")) {
			return 1;
		}
		return 0; // healthy
	}

	static void writeStringToFile(String data, File file) throws IOException {
		FileWriter fileWriter = new FileWriter(file, false);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		bufferedWriter.write(data);
		bufferedWriter.flush();
		bufferedWriter.close();
		fileWriter.close();
	}

	private String showTemplate = """
			---
			Date: "%sT%s"
			lastmod: "%s"
			PublishDate: "%s"
			type: "tv-episode"
			title: "Spring Office Hours: Episode %s - %s"
			episode: "%s"
			explicit: 'no'
			hosts:
			- DaShaun Carter
			- Dan Vega
			# guests:
			# -
			minutes: 60
			youtube: "%s"
			---

			Join Dan Vega and DaShaun Carter as they explore what’s new in the world of Spring. This is your chance to stay connected to what’s happening with the Spring Framework, related projects, and the community. During this live show, Dan and DaShaun will review the current news, demo a Spring related project they find interesting and answer any questions you might have.
			""";

}

interface ImageServiceClient {

	@GetExchange("/template")
	byte[] getTemplateImage();

	@GetExchange("/streamyard")
	byte[] getStreamyardImage();

	@PostExchange("/custom")
	byte[] getCustomSpringOfficeHours(@RequestBody String copy);

	@GetExchange("/actuator/health")
	String serviceHealth();

}

@Configuration(proxyBeanMethods = false)
class ImageServiceClientConfig {

	private final ConfigProps configProps;

	public ImageServiceClientConfig(ConfigProps configProps) {
		this.configProps = configProps;
	}

	@Bean
	public HttpServiceProxyFactory httpServiceProxyFactory() {
		HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(30));

		WebClient client = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.baseUrl(configProps.url())
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
			.defaultHeader("Content-Type", "text/plain")

			.build();
		return HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client)).build();
	}

	@Bean
	public ImageServiceClient imageServiceClient(HttpServiceProxyFactory factory) {
		return factory.createClient(ImageServiceClient.class);
	}

}

@ConfigurationProperties("image.service")
record ConfigProps(String url) {

}