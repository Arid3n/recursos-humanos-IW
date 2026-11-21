window.DEMO = {
 "duration": 847.6,
 "chapters": [
  {
   "hu": "Inicio",
   "title": "Presentación del sistema",
   "text": "Sistema de Gestión de Recursos Humanos · Spec-Driven Development · 8 historias de usuario",
   "t": 1.8,
   "intro": false
  },
  {
   "hu": "HU-01",
   "title": "Introducción: Alta de empleados y trayectoria contractual",
   "text": "Alta de empleados y trayectoria contractual",
   "t": 12.0,
   "intro": true
  },
  {
   "hu": "HU-01",
   "title": "Acceso con credenciales incorrectas",
   "text": "Escenario de error: el sistema rechaza una contraseña incorrecta",
   "t": 18.7,
   "intro": false
  },
  {
   "hu": "HU-01",
   "title": "Acceso como RRHH",
   "text": "RRHH inicia sesión con sus credenciales",
   "t": 28.9,
   "intro": false
  },
  {
   "hu": "HU-01",
   "title": "Validación del formulario de alta",
   "text": "Escenario de error: campos obligatorios y formato de NIF/NIE y correo",
   "t": 38.8,
   "intro": false
  },
  {
   "hu": "HU-01",
   "title": "Alta correcta con responsable",
   "text": "Flujo principal: alta con puesto, departamento, convenio TIC y responsable Carlos Ruiz",
   "t": 52.3,
   "intro": false
  },
  {
   "hu": "HU-01",
   "title": "Alta duplicada",
   "text": "Escenario de error: el NIF y el correo ya existen, el alta se rechaza",
   "t": 89.4,
   "intro": false
  },
  {
   "hu": "HU-01",
   "title": "Responsable obligatorio",
   "text": "Regla de negocio: sin responsable solo se admite la excepción explícita «Dirección General»",
   "t": 120.5,
   "intro": false
  },
  {
   "hu": "HU-01",
   "title": "Trayectoria contractual inmutable",
   "text": "Variación retroactiva con fecha de efectos, historial append-only y rechazo de fechas duplicadas",
   "t": 128.5,
   "intro": false
  },
  {
   "hu": "HU-02",
   "title": "Introducción: Ficha personal y saldo de vacaciones",
   "text": "Ficha personal y saldo de vacaciones",
   "t": 155.9,
   "intro": true
  },
  {
   "hu": "HU-02",
   "title": "Ficha y saldo de la empleada",
   "text": "Laura Martínez entra y consulta su ficha y su saldo de vacaciones del año",
   "t": 161.4,
   "intro": false
  },
  {
   "hu": "HU-02",
   "title": "Consulta de años anteriores",
   "text": "El selector permite consultar el saldo de ejercicios cerrados",
   "t": 179.5,
   "intro": false
  },
  {
   "hu": "HU-02",
   "title": "Saldo prorrateado por fecha de alta",
   "text": "Pedro se incorporó el 1 de julio: sus días correspondientes se prorratean",
   "t": 187.6,
   "intro": false
  },
  {
   "hu": "HU-02",
   "title": "Privacidad del saldo",
   "text": "Escenario de permisos: un empleado no puede consultar el saldo de otra persona",
   "t": 195.5,
   "intro": false
  },
  {
   "hu": "HU-03",
   "title": "Introducción: Solicitud de vacaciones o permisos",
   "text": "Solicitud de vacaciones o permisos",
   "t": 209.3,
   "intro": true
  },
  {
   "hu": "HU-03",
   "title": "Fecha pasada rechazada",
   "text": "Escenario de error: no se puede solicitar un periodo que ya ha comenzado",
   "t": 217.8,
   "intro": false
  },
  {
   "hu": "HU-03",
   "title": "Permiso sin justificación",
   "text": "Escenario de error: un permiso exige justificación",
   "t": 235.8,
   "intro": false
  },
  {
   "hu": "HU-03",
   "title": "Saldo insuficiente",
   "text": "Escenario de error: se pide más de lo que queda disponible (409)",
   "t": 253.9,
   "intro": false
  },
  {
   "hu": "HU-03",
   "title": "Solicitud de vacaciones válida",
   "text": "Flujo principal: del 14 al 18 de diciembre; el servidor calcula los días laborables",
   "t": 273.9,
   "intro": false
  },
  {
   "hu": "HU-03",
   "title": "Solapamiento con otra solicitud",
   "text": "Escenario de error: el periodo coincide con una solicitud ya registrada",
   "t": 294.9,
   "intro": false
  },
  {
   "hu": "HU-03",
   "title": "Permiso con justificación",
   "text": "Flujo alternativo: un día de permiso retribuido con su justificación",
   "t": 314.5,
   "intro": false
  },
  {
   "hu": "HU-04",
   "title": "Introducción: Seguimiento y cancelación de solicitudes",
   "text": "Seguimiento y cancelación de solicitudes",
   "t": 334.3,
   "intro": true
  },
  {
   "hu": "HU-04",
   "title": "Historial de solicitudes",
   "text": "Todas las solicitudes con su estado, periodo, días y justificación",
   "t": 343.3,
   "intro": false
  },
  {
   "hu": "HU-04",
   "title": "Cancelación de una solicitud pendiente",
   "text": "Flujo principal: la empleada cancela su solicitud del 11 al 13 de enero",
   "t": 351.0,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Introducción: Aprobación y rechazo de solicitudes",
   "text": "Aprobación y rechazo de solicitudes",
   "t": 375.5,
   "intro": true
  },
  {
   "hu": "HU-05",
   "title": "Bandeja de solicitudes pendientes",
   "text": "Carlos Ruiz ve las solicitudes de Laura y de Pedro",
   "t": 394.6,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Rechazo con motivo demasiado corto",
   "text": "Escenario de error: el motivo de rechazo exige al menos 5 caracteres",
   "t": 399.0,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Rechazo con motivo válido",
   "text": "Flujo alternativo: se rechaza el permiso indicando el motivo",
   "t": 408.5,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Aprobación con comentario",
   "text": "Flujo principal: se aprueban las vacaciones de Laura con un comentario",
   "t": 416.9,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Aprobación de un permiso",
   "text": "Los permisos se aprueban igual; no descuentan días de vacaciones",
   "t": 427.9,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Aprobación de las vacaciones de Pedro",
   "text": "Pedro coincide con Laura del 16 al 18 de diciembre: se aprueba para ver la coincidencia en el calendario",
   "t": 436.4,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Resolución doble y fuera de alcance",
   "text": "Escenarios de error: una solicitud ya resuelta devuelve 409; un empleado no puede resolver (403)",
   "t": 445.2,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Cancelar una solicitud ya resuelta (regla de HU-04)",
   "text": "Regla de HU-04: una solicitud ya resuelta no se puede cancelar, solo las pendientes",
   "t": 453.0,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Notificación al empleado",
   "text": "Laura recibe una notificación por cada resolución",
   "t": 480.9,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Motivo del rechazo visible",
   "text": "La empleada consulta por qué se rechazó su permiso",
   "t": 489.9,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Cadena de responsables",
   "text": "Un responsable no resuelve su propia solicitud: la resuelve su superior (RRHH), y la de RRHH, Dirección General",
   "t": 515.8,
   "intro": false
  },
  {
   "hu": "HU-05",
   "title": "Dirección General no solicita ausencias",
   "text": "Regla de negocio: sin superior que apruebe, Dirección General no puede solicitar ausencias ni tiene saldo propio",
   "t": 566.9,
   "intro": false
  },
  {
   "hu": "HU-06",
   "title": "Introducción: Calendario de ausencias del equipo",
   "text": "Calendario de ausencias del equipo",
   "t": 580.9,
   "intro": true
  },
  {
   "hu": "HU-06",
   "title": "Calendario del mes actual",
   "text": "Noviembre: sin ausencias en el equipo",
   "t": 600.0,
   "intro": false
  },
  {
   "hu": "HU-06",
   "title": "Coincidencias en diciembre",
   "text": "Laura y Pedro coinciden del 16 al 18 de diciembre: el calendario resalta los días",
   "t": 604.1,
   "intro": false
  },
  {
   "hu": "HU-06",
   "title": "Filtro por empleado",
   "text": "El responsable filtra el calendario por una persona del equipo",
   "t": 613.9,
   "intro": false
  },
  {
   "hu": "HU-06",
   "title": "Detalle de una ausencia",
   "text": "Al abrir una ausencia se ve la persona, el tipo y las fechas, pero no su justificación",
   "t": 626.9,
   "intro": false
  },
  {
   "hu": "HU-06",
   "title": "Vista adaptada a móvil",
   "text": "Por debajo de 600 px el calendario se presenta como lista vertical de días",
   "t": 634.2,
   "intro": false
  },
  {
   "hu": "HU-06",
   "title": "Alcance por jerarquía",
   "text": "Escenario de permisos: un empleado sin personas a su cargo no ve el calendario del equipo",
   "t": 642.7,
   "intro": false
  },
  {
   "hu": "HU-07",
   "title": "Introducción: Informes de absentismo y uso de vacaciones",
   "text": "Informes de absentismo y uso de vacaciones",
   "t": 650.1,
   "intro": true
  },
  {
   "hu": "HU-07",
   "title": "Informe de absentismo",
   "text": "Absentismo del año en curso por departamento",
   "t": 671.9,
   "intro": false
  },
  {
   "hu": "HU-07",
   "title": "Uso de vacaciones",
   "text": "Cambio de tipo de informe: días correspondientes, disfrutados y saldo remanente",
   "t": 682.5,
   "intro": false
  },
  {
   "hu": "HU-07",
   "title": "Exportación a CSV",
   "text": "El CSV se genera con los mismos datos que se ven en pantalla",
   "t": 689.7,
   "intro": false
  },
  {
   "hu": "HU-07",
   "title": "Periodo sin datos",
   "text": "Escenario límite: un rango sin ausencias muestra «No hay datos para el periodo seleccionado»",
   "t": 696.9,
   "intro": false
  },
  {
   "hu": "HU-07",
   "title": "Acceso restringido a RRHH",
   "text": "Escenario de permisos: un empleado no ve la opción Informes y el servidor responde 403",
   "t": 705.7,
   "intro": false
  },
  {
   "hu": "HU-08",
   "title": "Introducción: Evaluación del desempeño con conformidad",
   "text": "Evaluación del desempeño con conformidad",
   "t": 732.2,
   "intro": true
  },
  {
   "hu": "HU-08",
   "title": "Nueva evaluación",
   "text": "Carlos crea la evaluación anual de Laura con valoración 4 «Supera expectativas»",
   "t": 751.3,
   "intro": false
  },
  {
   "hu": "HU-08",
   "title": "Valoración fuera de rango",
   "text": "Escenario de error: la valoración solo admite valores del 1 al 5",
   "t": 770.9,
   "intro": false
  },
  {
   "hu": "HU-08",
   "title": "Conformidad del empleado",
   "text": "Laura revisa la evaluación, añade un comentario y da su conformidad",
   "t": 795.3,
   "intro": false
  },
  {
   "hu": "HU-01",
   "title": "Baja lógica de un empleado",
   "text": "Epílogo: la baja desactiva la cuenta y conserva el historial",
   "t": 820.2,
   "intro": false
  }
 ]
};
