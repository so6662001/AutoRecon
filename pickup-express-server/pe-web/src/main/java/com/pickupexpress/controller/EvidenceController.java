package com.pickupexpress.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickupexpress.common.result.R;
import com.pickupexpress.domain.vo.EvidencePackageVO;
import com.pickupexpress.service.EvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Tag(name = "证据归档")
@RestController
@RequestMapping("/api/v1/evidence/archive")
@RequiredArgsConstructor
@Slf4j
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "归档证据")
    @PostMapping("/{pickupOrderId}")
    public R<Long> archiveEvidence(@PathVariable Long pickupOrderId) {
        Long id = evidenceService.archiveEvidence(pickupOrderId);
        return R.ok(id);
    }

    @Operation(summary = "获取证据包")
    @GetMapping("/{pickupOrderId}")
    public R<EvidencePackageVO> getEvidencePackage(@PathVariable Long pickupOrderId) {
        EvidencePackageVO vo = evidenceService.getEvidencePackage(pickupOrderId);
        return R.ok(vo);
    }

    @Operation(summary = "验证证据完整性")
    @GetMapping("/{pickupOrderId}/verify")
    public R<Boolean> verifyIntegrity(@PathVariable Long pickupOrderId) {
        boolean valid = evidenceService.verifyIntegrityByPickupOrderId(pickupOrderId);
        return R.ok(valid);
    }

    @Operation(summary = "下载证据包(ZIP)")
    @GetMapping("/{pickupOrderId}/download")
    public void downloadZip(@PathVariable Long pickupOrderId, HttpServletResponse response) throws IOException {
        EvidencePackageVO vo = evidenceService.getEvidencePackage(pickupOrderId);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=evidence_" + pickupOrderId + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            zos.putNextEntry(new ZipEntry("summary.json"));
            zos.write(objectMapper.writeValueAsString(vo).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            addPlaceholder(zos, "contract_pdf.txt", vo.getContractPdfUrl(), "contract PDF");
            addPlaceholder(zos, "pickup_order_pdf.txt", vo.getPickupOrderPdfUrl(), "pickup order PDF");
            addPlaceholder(zos, "delivery_data.txt", vo.getDeliveryDataUrl(), "delivery data");
            addPlaceholder(zos, "signature.txt", vo.getSignatureUrl(), "signature image");
            addPlaceholder(zos, "settlement_pdf.txt", vo.getSettlementPdfUrl(), "settlement PDF");

            List<String> photoUrls = splitUrls(vo.getPhotosUrls());
            for (int i = 0; i < photoUrls.size(); i++) {
                addPlaceholder(zos, "photo_" + (i + 1) + ".txt", photoUrls.get(i), "delivery photo");
            }
        }
    }

    private static void addPlaceholder(ZipOutputStream zos, String entryName, String url, String label) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        String text = url == null || url.isBlank()
                ? "[placeholder] " + label + " not available\n"
                : "[placeholder] " + label + " URL: " + url + "\n";
        zos.write(text.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static List<String> splitUrls(String photosUrls) {
        if (photosUrls == null || photosUrls.isBlank()) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (String part : photosUrls.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }
}
