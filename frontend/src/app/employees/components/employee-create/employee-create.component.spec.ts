import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { EmployeeCreateComponent } from './employee-create.component';
import { EmployeeService } from '../../services/employee.service';

/** HU-05 FR-017: «Sin responsable» es una elección explícita (withoutManager) y nunca el valor por defecto. */
describe('EmployeeCreateComponent (responsable)', () => {
  let employeeService: { listEmployees: ReturnType<typeof vi.fn>; createEmployee: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    employeeService = {
      listEmployees: vi.fn().mockReturnValue(of({ content: [] })),
      createEmployee: vi.fn().mockReturnValue(
        of({ id: 'e-1', email: 'a@b.com', fullName: 'A B', nationalId: '12345678Z', contractRecordId: 1, createdAt: '' })
      )
    };
    await TestBed.configureTestingModule({
      imports: [EmployeeCreateComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'employees/new', component: EmployeeCreateComponent }]),
        { provide: EmployeeService, useValue: employeeService }
      ]
    }).compileComponents();
  });

  function filledComponent(managerId: string | null) {
    const fixture = TestBed.createComponent(EmployeeCreateComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    // El componente trae su propio MatDialog (MatDialogModule): se espía en la instancia
    vi.spyOn((component as unknown as { dialog: { open: () => unknown } }).dialog, 'open')
      .mockReturnValue({ afterClosed: () => of(undefined) });
    component.form.patchValue({
      firstName: 'Ana',
      lastName: 'López',
      nationalId: '12345678Z',
      email: 'ana@empresa.com',
      hireDate: '2026-11-01',
      position: 'Analista',
      department: 'Tecnología',
      salary: 30000,
      collectiveAgreementId: 1,
      managerId
    });
    return component;
  }

  it('does not allow submitting without choosing a responsible (the field starts unchosen)', () => {
    const component = filledComponent(null);

    expect(component.form.invalid).toBe(true);
    component.onSubmit();
    expect(employeeService.createEmployee).not.toHaveBeenCalled();
  });

  it('sends withoutManager=true and no managerId when «Sin responsable» is chosen', () => {
    const component = filledComponent('NONE');

    component.onSubmit();

    const payload = employeeService.createEmployee.mock.calls[0][0];
    expect(payload.withoutManager).toBe(true);
    expect(payload.managerId).toBeNull();
  });

  it('sends the managerId and withoutManager=false when a responsible is chosen', () => {
    const component = filledComponent('11111111-1111-1111-1111-111111111111');

    component.onSubmit();

    const payload = employeeService.createEmployee.mock.calls[0][0];
    expect(payload.managerId).toBe('11111111-1111-1111-1111-111111111111');
    expect(payload.withoutManager).toBe(false);
  });
});
