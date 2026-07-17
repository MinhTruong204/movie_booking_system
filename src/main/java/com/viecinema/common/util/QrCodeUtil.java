package com.viecinema.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class de sinh anh QR Code dang byte[] (PNG).
 * Su dung Google ZXing library.
 */
@Slf4j
@UtilityClass
public class QrCodeUtil {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;
    private static final String FORMAT = "PNG";

    /**
     * Sinh anh QR Code tu chuoi noi dung.
     *
     * @param content noi dung can ma hoa
     * @param width   chieu rong anh (pixel)
     * @param height  chieu cao anh (pixel)
     * @return mang byte anh QR Code dinh dang PNG
     */
    public byte[] generateQrCode(String content, int width, int height)
            throws WriterException, IOException {

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, FORMAT, outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Sinh anh QR Code voi kich thuoc mac dinh 300x300px.
     *
     * @param content noi dung can ma hoa
     * @return mang byte anh QR Code dinh dang PNG
     */
    public byte[] generateQrCode(String content) throws WriterException, IOException {
        return generateQrCode(content, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
}