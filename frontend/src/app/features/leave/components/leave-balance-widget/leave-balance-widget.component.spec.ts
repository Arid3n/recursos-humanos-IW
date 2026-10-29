import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { LeaveBalanceWidgetComponent } from './leave-balance-widget.component';
import { LeaveService } from '../../services/leave.service';
import { LeaveBalanceResponse } from '../../models/leave.models';

describe('LeaveBalanceWidgetComponent', () => {
  let mockLeaveService: Partial<LeaveService>;

  const mockBalance: LeaveBalanceResponse = {
    employeeId: '123e4567-e89b-12d3-a456-426614174000',
    year: 2026,
    entitledDays: 22,
    spentDays: 5,
    pendingDays: 2,
    availableDays: 17,
    isClosedYear: false,
    agreementName: 'Convenio TIC'
  };

  beforeEach(async () => {
    mockLeaveService = {
      getMyLeaveBalance: vi.fn().mockReturnValue(of(mockBalance)),
      getMyLeaveRequests: vi.fn().mockReturnValue(of([]))
    };

    await TestBed.configureTestingModule({
      imports: [LeaveBalanceWidgetComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LeaveService, useValue: mockLeaveService }
      ]
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(LeaveBalanceWidgetComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should render all 4 balance cards with exact values and WCAG aria-labels', () => {
    const fixture = TestBed.createComponent(LeaveBalanceWidgetComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const region = compiled.querySelector('[role="region"]');
    expect(region).toBeTruthy();
    expect(region?.getAttribute('aria-label')).toBe('Saldo y balance de vacaciones');

    const entitledCard = compiled.querySelector('.metric-card.entitled');
    expect(entitledCard).toBeTruthy();
    expect(entitledCard?.textContent).toContain('22');
    expect(entitledCard?.getAttribute('aria-label')).toContain('22');

    const spentCard = compiled.querySelector('.metric-card.spent');
    expect(spentCard).toBeTruthy();
    expect(spentCard?.textContent).toContain('5');

    const availableCard = compiled.querySelector('.metric-card.available');
    expect(availableCard).toBeTruthy();
    expect(availableCard?.textContent).toContain('17');

    const pendingCard = compiled.querySelector('.metric-card.pending');
    expect(pendingCard).toBeTruthy();
    expect(pendingCard?.textContent).toContain('2');
  });

  it('should display closed-year banner when isClosedYear is true', () => {
    const closedMockBalance: LeaveBalanceResponse = {
      ...mockBalance,
      year: 2025,
      isClosedYear: true
    };
    (mockLeaveService.getMyLeaveBalance as any).mockReturnValue(of(closedMockBalance));

    const fixture = TestBed.createComponent(LeaveBalanceWidgetComponent);
    fixture.componentInstance.selectedYear.set(2025);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const banner = compiled.querySelector('.closed-year-banner');
    expect(banner).toBeTruthy();
    expect(banner?.textContent).toContain('Ejercicio fiscal 2025 consolidado');
  });
});
