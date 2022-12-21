package dev.dashaun.cli.soh;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.Availability;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;


@SpringBootApplication
public class Application {

    static {
        ImageIO.scanForPlugins();
    }
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

@Component
record CustomPromptProvider() implements PromptProvider {

    @Override
    public AttributedString getPrompt() {
        return new AttributedString("spring-office-hours:>",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
    }

}

@ShellComponent
record ImageCommands() {

    private static final String FONT = "HWT-Art-W00-Regular.ttf";
    private static final String TEMPLATE = "spring-office-hours-blank.png";

    private static final int LEFT_PAD = 60;
    private static final int BOTTOM_PAD = 60;

    @ShellMethod("create episode image from template and text")
    @ShellMethodAvailability("templateFile")
    public String episodeImages(@ShellOption(defaultValue = "Episode 0000") String text, @ShellOption(defaultValue = "./content/tv/spring-office-hours/example.png") String path) throws FontFormatException {
        try {
            BufferedImage image = ImageIO.read(template());
            Font font = fit(getFont(), text, image);
            java.text.AttributedString attributedText = new java.text.AttributedString(text);

            attributedText.addAttribute(TextAttribute.FONT, font);
            attributedText.addAttribute(TextAttribute.FOREGROUND, Color.BLACK);
            Graphics g = image.getGraphics();

            FontMetrics metrics = g.getFontMetrics(font);
            int positionY = (image.getHeight() - metrics.getHeight() - BOTTOM_PAD) + metrics.getAscent();

            g.drawString(attributedText.getIterator(), LEFT_PAD, positionY);

            ImageIO.write(image, "png", new File(path));
        } catch (IOException ioException) {
            return "There was a problem: " + ioException.getMessage();
        }
        return "Success";
    }

    private Font fit(Font baseFont, String text, BufferedImage image) throws IOException {
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

    public Availability templateFile() {
        try (InputStream is = template()) {
            return is != null
                    ? Availability.available()
                    : Availability.unavailable(String.format("%s does not exist", TEMPLATE));
        } catch (IOException ioe) {
            return Availability.unavailable(ioe.getMessage());
        }
    }

    private InputStream template() {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResourceAsStream(TEMPLATE);
    }

    private Font getFont() throws IOException, FontFormatException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream(FONT);
        assert is != null;
        Font ttf = Font.createFont(Font.TRUETYPE_FONT, is);
        return ttf.deriveFont(ttf.getSize2D() * 80);
    }
}