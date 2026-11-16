package com.banking.platform.transaction;

import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    public String classify(String description, String merchant) {
        String s = (description + " " + merchant).toUpperCase(Locale.ROOT);
        if (s.contains("SALARY") || s.contains("EMPLOYER")) return "INCOME";
        if (s.contains("GROCERY") || s.contains("MARKET")) return "GROCERIES";
        if (s.contains("FUEL") || s.contains("TRANSPORT") || s.contains("UBER")) return "TRANSPORT";
        if (s.contains("CAFE") || s.contains("RESTAURANT") || s.contains("LUNCH")) return "DINING";
        if (s.contains("SHOPPING") || s.contains("STORE")) return "SHOPPING";
        return "OTHER";
    }
}
