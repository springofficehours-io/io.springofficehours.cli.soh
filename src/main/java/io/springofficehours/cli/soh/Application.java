package io.springofficehours.cli.soh;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.jline.PromptProvider;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
public class Application {

	private static final String FONT = "HWT-Art-W00-Regular.ttf";

	private static final String STREAMYARD = "spring-office-hours-streamyard.png";

	private static final String TEMPLATE = "spring-office-hours-blank.png";

	private static final int LEFT_PAD = 60;

	private static final int BOTTOM_PAD = 60;

	static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Command(name = "streamyard-image", description = "Generate image to add when scheduling in Streamyard")
	void streamyardImage(@Option(shortName = 't', longName = "text", description = "Text for image (optional)",
			defaultValue = "!") String theText) throws IOException, FontFormatException {
		BufferedImage image = ImageIO.read(templateInputStream(TEMPLATE));
		// Set the font to the right size, before the text is overlayed onto the image
		Font font = fit(getFont(), theText, image);
		java.text.AttributedString attributedText = new java.text.AttributedString(theText);

		attributedText.addAttribute(TextAttribute.FONT, font);
		attributedText.addAttribute(TextAttribute.FOREGROUND, Color.BLACK);
		Graphics g = image.getGraphics();

		FontMetrics metrics = g.getFontMetrics(font);
		int positionY = (image.getHeight() - metrics.getHeight() - BOTTOM_PAD) + metrics.getAscent();

		g.drawString(attributedText.getIterator(), LEFT_PAD, positionY);

		// Write to file in current working directory
		File outputFile = new File("streamyard.png");
		ImageIO.write(image, "png", outputFile);

	}

	@Bean
	PromptProvider CustomPromptProvider() {
		return () -> new AttributedString("spring-office-hours:>",
				AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
	}

	private Font fit(Font baseFont, String text, BufferedImage image) {
		Font newFont = baseFont;

		FontMetrics ruler = image.getGraphics().getFontMetrics(baseFont);
		GlyphVector vector = baseFont.createGlyphVector(ruler.getFontRenderContext(), text + "___");

		Shape outline = vector.getOutline(0, 0);

		double expectedWidth = outline.getBounds().getWidth();
		double expectedHeight = outline.getBounds().getHeight();

		boolean textFits = image.getWidth() >= expectedWidth && image.getHeight() >= expectedHeight;

		if (!textFits) {
			double widthBasedFontSize = (baseFont.getSize2D() * image.getWidth()) / expectedWidth;
			double heightBasedFontSize = (baseFont.getSize2D() * image.getHeight()) / expectedHeight;

			double newFontSize = Math.min(widthBasedFontSize, heightBasedFontSize);
			newFont = baseFont.deriveFont(baseFont.getStyle(), (float) newFontSize);
		}
		return newFont;
	}

	private InputStream templateInputStream(String file) {
		ClassLoader classLoader = getClass().getClassLoader();
		return classLoader.getResourceAsStream(file);
	}

	private Font getFont() throws IOException, FontFormatException {
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream is = classLoader.getResourceAsStream(FONT);
		assert is != null;
		Font ttf = Font.createFont(Font.TRUETYPE_FONT, is);
		return ttf.deriveFont(ttf.getSize2D() * 80);
	}

}