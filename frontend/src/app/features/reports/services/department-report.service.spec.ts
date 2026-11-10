import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DepartmentReportService } from './department-report.service';

describe('DepartmentReportService', () => {
  let service: DepartmentReportService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DepartmentReportService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the same report type and date range for display and CSV export', () => {
    service.getReport('VACATION_USAGE', '2026-01-01', '2026-12-31').subscribe();
    const reportRequest = http.expectOne((request) => request.url === '/api/v1/reports');
    expect(reportRequest.request.params.get('type')).toBe('VACATION_USAGE');
    expect(reportRequest.request.params.get('startDate')).toBe('2026-01-01');
    expect(reportRequest.request.params.get('endDate')).toBe('2026-12-31');
    reportRequest.flush({
      type: 'VACATION_USAGE',
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      periodLabel: '2026-01-01 – 2026-12-31',
      hasData: true,
      departments: []
    });

    service.exportCsv('VACATION_USAGE', '2026-01-01', '2026-12-31').subscribe();
    const exportRequest = http.expectOne((request) => request.url === '/api/v1/reports/export');
    expect(exportRequest.request.responseType).toBe('blob');
    expect(exportRequest.request.params.get('type')).toBe('VACATION_USAGE');
    expect(exportRequest.request.params.get('startDate')).toBe('2026-01-01');
    expect(exportRequest.request.params.get('endDate')).toBe('2026-12-31');
    exportRequest.flush(new Blob(['csv']));
  });
});
