/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.result.verifycode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.result.AbstractResultHandler;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.result.structure.StructureResultHandler;
import net.hasor.dataway.service.ResultInfoUtils;

/** Renders script text as a PNG verification code. Challenge generation and verification belong to the application. */
public class VerifyCodeResultHandler extends AbstractResultHandler {
    public VerifyCodeResultHandler() {
        this(Map.of());
    }

    public VerifyCodeResultHandler(Map<String, ?> defaults) {
        super(defaults);
    }

    @Override
    public ResultInfo handle(ResultContext context) throws IOException {
        if (!context.isSuccess()) {
            return new StructureResultHandler().handle(context);
        }
        if (!(context.getValue() instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("VerifyCode result must be a non-blank string");
        }

        int[] characters = text.codePoints().toArray();
        if (characters.length > 32 || text.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("VerifyCode text must contain at most 32 characters and no control characters");
        }

        int width = characters.length * 32 + 32;
        int height = 64;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(245, 248, 252));
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 8; i++) {
                graphics.setColor(new Color(random.nextInt(150, 220), random.nextInt(150, 220), random.nextInt(150, 220)));
                graphics.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
            for (int i = 0; i < characters.length; i++) {
                String character = new String(Character.toChars(characters[i]));
                int center = i * 32 + 32;
                double angle = random.nextDouble(-0.2, 0.2);
                graphics.setColor(new Color(random.nextInt(20, 110), random.nextInt(20, 110), random.nextInt(20, 110)));
                graphics.rotate(angle, center, height / 2.0);
                graphics.drawString(character, center - graphics.getFontMetrics().stringWidth(character) / 2, 44);
                graphics.rotate(-angle, center, height / 2.0);
            }
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) {
            throw new IOException("PNG encoder is unavailable");
        }
        ResultInfo response = ResultInfoUtils.binary("image/png", output.toByteArray());
        response.getHeaders().put("Cache-Control", "no-store");
        return response;
    }
}
