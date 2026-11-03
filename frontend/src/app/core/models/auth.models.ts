export type UserRole = 'RRHH' | 'EMPLEADO' | 'RESPONSABLE_EQUIPO';

export interface CurrentUser {
  id: string;
  username: string;
  role: UserRole;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  id: string;
  username: string;
  role: UserRole;
  token: string;
}

/** Nombre visible de cada rol en la interfaz. */
export const ROLE_LABELS: Record<UserRole, string> = {
  RRHH: 'RRHH',
  RESPONSABLE_EQUIPO: 'Responsable de equipo',
  EMPLEADO: 'Empleado'
};
