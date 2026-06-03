package pt.estga.vision.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.estga.vision.VisionClient;
import pt.estga.vision.dtos.VisionHealthResponse;

@RestController
@RequestMapping("/api/v1/public/vision")
@RequiredArgsConstructor
public class VisionHealthController {

    private final VisionClient visionClient;

    @GetMapping("/health")
    public ResponseEntity<VisionHealthResponse> health() {
        if (visionClient.isAvailable()) {
            return ResponseEntity.ok(VisionHealthResponse.up());
        }
        return ResponseEntity.status(503).body(VisionHealthResponse.down("Vision service is not reachable"));
    }
}
