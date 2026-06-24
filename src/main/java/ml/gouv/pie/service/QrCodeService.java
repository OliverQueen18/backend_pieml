package ml.gouv.pie.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class QrCodeService {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public byte[] generatePng(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Échec génération QR code : {}", e.getMessage());
            throw new IllegalStateException("Impossible de générer le QR code");
        }
    }

    public String buildTrackUrl(String referenceNumber) {
        String encoded = URLEncoder.encode(referenceNumber, StandardCharsets.UTF_8);
        return frontendUrl + "/suivre-dossier?ref=" + encoded;
    }
}
