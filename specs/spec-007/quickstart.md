# HU-07 — Informes de absentismo y uso de vacaciones

## Acceso desde la aplicación

Inicia PostgreSQL y el backend y frontend siguiendo los pasos del [README principal](../../README.md). Inicia sesión con un usuario de Recursos Humanos y abre **Informes** en la navegación o visita `http://localhost:4200/reports`.

Selecciona absentismo o uso de vacaciones y uno de estos periodos: año en curso, año anterior, trimestre actual o rango personalizado. Pulsa **Generar informe** para ver el agregado por departamento. Cada departamento se puede desplegar para consultar los empleados incluidos. Si no existen solicitudes aprobadas del tipo y periodo elegidos, se muestra un aviso y no una tabla vacía.

## API

La API requiere autenticación y rol `RRHH`:

```text
GET /api/v1/reports?type=ABSENTEEISM&startDate=2026-01-01&endDate=2026-12-31
GET /api/v1/reports/export?type=ABSENTEEISM&startDate=2026-01-01&endDate=2026-12-31
```

`type` admite `ABSENTEEISM` y `VACATION_USAGE`. La exportación devuelve CSV RFC 4180 con BOM UTF-8 y filas tanto departamentales como individuales; ambos endpoints utilizan el mismo cálculo y filtros. Para un periodo sin registros, la API de consulta devuelve `hasData: false` y la exportación responde `204 No Content`.

## Cálculos

- Solo se incluyen solicitudes `APPROVED`: permisos para absentismo y vacaciones para uso de vacaciones.
- Los días de solicitudes que cruzan el rango se limitan a los días laborables que caen dentro de él; fines de semana y festivos siguen el calendario laboral del backend.
- El absentismo es días aprobados de permisos divididos por los días laborables teóricos de la plantilla durante el periodo.
- El uso de vacaciones es días aprobados de vacaciones divididos por la asignación anual de cada empleado para cada año incluido en el rango. El año de alta se prorratea con la misma regla mensual que el saldo de vacaciones.
- El porcentaje departamental es el cociente de los totales departamentales. Con denominador cero, el porcentaje es `0.00`.
