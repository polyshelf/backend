package ru.polyshelf.gateway.scan.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.polyshelf.gateway.scan.model.ScanResult;
import ru.polyshelf.gateway.scan.service.ScanService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    @PostMapping("/scan")
    public ScanResult scan(@Valid @RequestBody ScanRequest request) {
        return scanService.scan(request.code());
    }

    @PostMapping("/cz-response")
    public ScanResult czResponse(@Valid @RequestBody CzResponseRequest request) {
        return scanService.acceptCzResponse(request.payload());
    }
}
