# Plan de Implementación: App NotiVas

Este plan detalla la creación de una aplicación Android para notificaciones de estudiantes integrada con la API de Canvas Instructure.

## Descripción del Objetivo
Desarrollar una aplicación que permita a los estudiantes:
1. Ingresar su institución y token de acceso de Canvas.
2. Verificar la conexión.
3. Visualizar tareas organizadas por Prioridad (tiempo restante), Completadas y Faltantes.
4. Filtrar tareas por curso.
5. Recibir notificaciones automáticas recordatorias (1 día antes por defecto) con opción a modificarlas.

## Cambios Propuestos

### 1. Configuración de Arquitectura y Dependencias
- Configurar **Hilt** para Inyección de Dependencias.
- Configurar **Retrofit** para llamadas a la API de Canvas.
- Configurar **Room** para caché local de tareas y cursos.
- Configurar **DataStore** para almacenamiento seguro de la URL de la universidad y el token.
- Configurar **WorkManager** para la programación de notificaciones.
- Configurar **Compose Navigation**.

### 2. Capa de Datos (Data Layer)
- **CanvasApiService**: Definir los endpoints de Canvas (`/api/v1/courses`, `/api/v1/users/self/upcoming_assignments`, etc.).
- **LocalDatabase**: Base de datos Room para persistir tareas y permitir funcionamiento offline parcial.
- **PreferencesManager**: Almacenar la configuración del usuario (Institución, Token, Preferencias de recordatorio).
- **CanvasRepository**: Unificar el acceso a datos remotos y locales.

### 3. Modelos de Dominio
- `Assignment`: Representa una tarea con fecha de entrega, estado y curso.
- `Course`: Representa una materia para facilitar el filtrado.

### 4. Capa de Interfaz (UI Layer - Jetpack Compose)
- **MainActivity / NavHost**: Gestionar el flujo entre pantallas.
- **Onboarding (Pantallas de Configuración)**:
    - `UniversityInputScreen`: Entrada del dominio de la universidad (ej. `canvas.instructure.com` o `miu.instructure.com`).
    - `TokenInputScreen`: Entrada del Access Token de Canvas.
    - `VerificationScreen`: Validar el token y descargar datos iniciales.
- **Dashboard (Pantalla Principal)**:
    - Secciones: **Prioritarias** (ordenadas por fecha próxima), **Completadas**, **Faltantes**.
    - Filtro de Curso: Menú desplegable o chips para filtrar la vista actual.
- **Configuración de Recordatorios**: Diálogo o pantalla para ajustar el tiempo de anticipación de las notificaciones.

### 5. Sistema de Notificaciones
- **ReminderWorker**: Tarea periódica o programada para revisar fechas de entrega y disparar notificaciones locales.
- **NotificationHelper**: Configurar canales de notificación y mostrar alertas.

---

## Plan de Verificación

### Pruebas Automatizadas
- Pruebas unitarias para el `CanvasRepository` y el filtrado de tareas.
- Pruebas de integración para la base de datos Room.

### Verificación Manual
- Ingresar un token real (o de prueba) para verificar la conexión.
- Validar que las tareas se ordenen correctamente por fecha de entrega.
- Probar el filtro por curso.
- Verificar la recepción de una notificación de prueba.
