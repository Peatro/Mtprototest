package com.peatroxd.mtprototest.admin.controller;

import com.peatroxd.mtprototest.admin.dto.AdminCatalogOverviewResponse;
import com.peatroxd.mtprototest.admin.dto.AdminProxyModerationRequest;
import com.peatroxd.mtprototest.admin.dto.AdminProxyModerationResponse;
import com.peatroxd.mtprototest.admin.service.ProxyAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class AdminProxyController {

    private final ProxyAdminService proxyAdminService;

    @GetMapping("/overview")
    public AdminCatalogOverviewResponse getOverview() {
        return proxyAdminService.getOverview();
    }

    @PatchMapping("/proxies/{proxyId}/moderation")
    public AdminProxyModerationResponse updateModerationStatus(
            @PathVariable Long proxyId,
            @Valid @RequestBody AdminProxyModerationRequest request
    ) {
        return proxyAdminService.updateModerationStatus(proxyId, request.moderationStatus());
    }
}
