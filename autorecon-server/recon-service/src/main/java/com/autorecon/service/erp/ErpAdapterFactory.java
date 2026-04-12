package com.autorecon.service.erp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ErpAdapterFactory {
    private final List<ErpDataAdapter> adapters;

    public Optional<ErpDataAdapter> getAdapter(int connectionType) {
        String type = switch (connectionType) {
            case 1 -> "REST";
            case 2 -> "WEBSERVICE";
            case 3 -> "DB";
            case 4 -> "FILE";
            default -> "REST";
        };
        return adapters.stream().filter(a -> a.getType().equals(type)).findFirst();
    }
}
