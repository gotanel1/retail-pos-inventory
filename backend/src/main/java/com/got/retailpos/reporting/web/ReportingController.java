package com.got.retailpos.reporting.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.reporting.application.ReportingService;
import com.got.retailpos.reporting.application.ReportingService.DashboardReport;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class ReportingController {

	private final ReportingService reporting;

	public ReportingController(ReportingService reporting) {
		this.reporting = reporting;
	}

	@GetMapping("/dashboard")
	public DashboardReport dashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return reporting.dashboard(from, to);
	}
}
