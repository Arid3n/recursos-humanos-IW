import { TestBed } from '@angular/core/testing';
import { NEVER } from 'rxjs';
import { DepartmentReportsComponent } from './department-reports.component';
import { DepartmentReportService } from '../../services/department-report.service';

describe('DepartmentReportsComponent', () => {
  const reportService = {
    getReport: vi.fn(),
    exportCsv: vi.fn().mockReturnValue(NEVER)
  };

  beforeEach(async () => {
    reportService.getReport.mockReset();
    reportService.exportCsv.mockReset().mockReturnValue(NEVER);
    await TestBed.configureTestingModule({
      imports: [DepartmentReportsComponent],
      providers: [{ provide: DepartmentReportService, useValue: reportService }]
    }).compileComponents();
  });

  it('exports the same report period and type currently displayed', () => {
    const fixture = TestBed.createComponent(DepartmentReportsComponent);
    const component = fixture.componentInstance;
    component.report.set({
      type: 'VACATION_USAGE',
      startDate: '2025-01-01',
      endDate: '2025-12-31',
      periodLabel: '2025-01-01 – 2025-12-31',
      hasData: true,
      departments: []
    });

    component.reportType.set('ABSENTEEISM');
    component.startDate.set('2026-01-01');
    component.endDate.set('2026-12-31');
    component.exportCsv();

    expect(reportService.exportCsv).toHaveBeenCalledWith(
      'VACATION_USAGE',
      '2025-01-01',
      '2025-12-31'
    );
  });
});
