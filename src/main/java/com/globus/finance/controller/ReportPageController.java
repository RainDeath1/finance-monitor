package com.globus.finance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class ReportPageController {

    @GetMapping("/reports")
    public String reports(Model model) {

        LocalDate today = LocalDate.now();

        model.addAttribute("year", today.getYear());
        model.addAttribute("month", today.getMonthValue());
        model.addAttribute(
                "quarter",
                (today.getMonthValue() - 1) / 3 + 1
        );

        return "reports";
    }
}
