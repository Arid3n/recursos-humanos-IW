import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TeamCalendar } from '../models/team-calendar.models';

@Injectable({
  providedIn: 'root'
})
export class TeamCalendarService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/leave/team-calendar';

  /** Calendario del equipo del responsable en sesión para un mes en formato YYYY-MM. */
  getTeamCalendar(month: string): Observable<TeamCalendar> {
    const params = new HttpParams().set('month', month);
    return this.http.get<TeamCalendar>(this.apiUrl, { params });
  }
}
