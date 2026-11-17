package core.utils.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import core.logging.Logger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Utility to generate Code 128 barcodes using ZXing.
 */
public class BarcodeGenerator {

    /**
     * Generates a Code 128 barcode as a BufferedImage with 0 margin.
     *
     * @param content The text/code to encode
     * @param width   Target width in pixels
     * @param height  Target height in pixels
     * @return Generated barcode image
     */
    public static BufferedImage generateCode128(String content, int width, int height) {
        if (content == null || content.trim().isEmpty()) {
            content = "UNKNOWN";
        }
        try {
            Code128Writer writer = new Code128Writer();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 0);

            BitMatrix bitMatrix = writer.encode(content.trim(), BarcodeFormat.CODE_128, width, height, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            Logger.errlog("Failed to generate Code128 barcode for: " + content, e);
            BufferedImage fallback = new BufferedImage(Math.max(width, 10), Math.max(height, 10), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = fallback.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK);
            g.drawString(content, 5, height / 2);
            g.dispose();
            return fallback;
        }
    }
}
