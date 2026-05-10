# Sistema de Biblioteca - Cloud Native v2.0
## DSY2207 - Desarrollo Cloud Native II - Semana 9 - Evaluación Final

---

## Arquitectura del Sistema

```
┌──────────────────────────────────────────────────────────────────┐
│                     CLIENTE (Postman / GraphiQL)                  │
└──────────┬───────────────────────────────────┬───────────────────┘
           │ REST /api/*                       │ GraphQL /graphql
           ▼                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│              microservicio-bff  :8080                            │
│   REST Controllers + Spring GraphQL (resolvers)                  │
└──────────┬────────────────────────────┬────────────────────────-┘
           │ REST                       │ REST
           ▼                            ▼
┌──────────────────┐         ┌──────────────────────┐
│ funcion-usuarios │         │  funcion-prestamos    │
│    :8081         │         │      :8082            │
│  REST + GraphQL  │         │   REST + GraphQL      │
└────────┬─────────┘         └──────────┬────────────┘
         │ AMQP eventos                 │ AMQP eventos
         └──────────────┬───────────────┘
                        ▼
               ┌─────────────────┐
               │   RabbitMQ      │
               │   :5672 / 15672 │
               └────────┬────────┘
                        │ consume
                        ▼
            ┌──────────────────────────┐
            │  microservicio-eventos   │
            │      :8083               │
            │  Lógica de negocio       │
            │  orientada a eventos     │
            └──────────────────────────┘
                        │ SQL
                        ▼
               ┌─────────────────┐
               │   Oracle XE     │
               │    :1521        │
               └─────────────────┘
```

## Componentes

| Servicio | Puerto | Tecnología | Descripción |
|---|---|---|---|
| `funcion-usuarios` | 8081 | Java + Spark + graphql-java | FaaS: CRUD Usuarios, REST + GraphQL |
| `funcion-prestamos` | 8082 | Java + Spark + graphql-java | FaaS: CRUD Préstamos, REST + GraphQL |
| `microservicio-bff` | 8080 | Spring Boot + Spring GraphQL | BFF: orquesta ambas funciones |
| `microservicio-eventos` | 8083 | Spring Boot + Spring AMQP | Consumidor de eventos RabbitMQ |
| `rabbitmq` | 5672/15672 | RabbitMQ 3.13 | Message Broker |
| `oracle-db` | 1521 | Oracle XE 21 | Base de datos |

---

## Levantar el sistema

```bash
# Construir y levantar todos los servicios
docker-compose up --build

# Ver logs en tiempo real
docker-compose logs -f

# Ver logs de un servicio específico (ver eventos procesados)
docker-compose logs -f microservicio-eventos
```

---

## Endpoints REST (via BFF)

| Método | URL | Descripción |
|---|---|---|
| GET | `http://localhost:8080/api/usuarios` | Listar usuarios |
| POST | `http://localhost:8080/api/usuarios` | Crear usuario |
| GET | `http://localhost:8080/api/usuarios/{id}` | Obtener usuario |
| PUT | `http://localhost:8080/api/usuarios/{id}` | Actualizar usuario |
| DELETE | `http://localhost:8080/api/usuarios/{id}` | Eliminar usuario (publica evento) |
| GET | `http://localhost:8080/api/prestamos` | Listar préstamos |
| POST | `http://localhost:8080/api/prestamos` | Crear préstamo (publica evento) |
| GET | `http://localhost:8080/api/prestamos/{id}` | Obtener préstamo |
| PUT | `http://localhost:8080/api/prestamos/{id}/devolver` | Devolver libro (publica evento) |
| DELETE | `http://localhost:8080/api/prestamos/{id}` | Eliminar préstamo |

---

## GraphQL (via BFF)

**URL:** `http://localhost:8080/graphql`  
**GraphiQL (interfaz web):** `http://localhost:8080/graphiql`

### Ejemplos de Queries

```graphql
# Listar todos los usuarios
query {
  usuarios {
    id nombre apellido email estado
  }
}

# Obtener un usuario específico
query {
  usuario(id: "1") {
    id nombre apellido email rut telefono estado
  }
}

# Listar todos los préstamos
query {
  prestamos {
    id idUsuario idLibro fechaPrestamo fechaDevolucionEsperada estado
  }
}

# Préstamos de un usuario
query {
  prestamosPorUsuario(idUsuario: "1") {
    id idLibro fechaPrestamo estado
  }
}
```

### Ejemplos de Mutations

```graphql
# Crear un usuario (genera evento USUARIO_CREADO en RabbitMQ)
mutation {
  crearUsuario(
    nombre: "Ana"
    apellido: "Martinez"
    email: "ana.martinez@ejemplo.cl"
    rut: "15987654-3"
    telefono: "+56955544433"
  ) {
    id nombre email estado
  }
}

# Crear un préstamo (genera evento PRESTAMO_CREADO → descuenta disponibilidad del libro)
mutation {
  crearPrestamo(
    idUsuario: "1"
    idLibro: "1"
    fechaDevolucionEsperada: "2026-06-30"
  ) {
    id idUsuario idLibro fechaPrestamo estado
  }
}

# Devolver un libro (genera evento PRESTAMO_DEVUELTO → incrementa disponibilidad)
mutation {
  devolverPrestamo(id: "1") {
    id estado fechaDevolucionReal
  }
}

# Eliminar usuario (genera evento USUARIO_ELIMINADO → elimina sus préstamos)
mutation {
  eliminarUsuario(id: "2")
}
```

---

## GraphQL directo en las funciones FaaS

Las funciones también exponen GraphQL directamente:

```bash
# Consulta GraphQL a funcion-usuarios directamente
curl -X POST http://localhost:8081/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ usuarios { id nombre email } }"}'

# Consulta GraphQL a funcion-prestamos directamente
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ prestamos { id idUsuario idLibro estado } }"}'
```

---

## Flujo de Eventos (Arquitectura Orientada a Eventos)

### Evento 1: Préstamo Creado
```
[Usuario crea préstamo] → funcion-prestamos
  → INSERT en BD
  → Publica "prestamo.creado" en RabbitMQ (exchange: biblioteca.events)
     → microservicio-eventos recibe evento
     → Decrementa cantidad_disponible del libro en 1
```

### Evento 2: Usuario Eliminado
```
[Usuario elimina usuario] → funcion-usuarios
  → DELETE en BD
  → Publica "usuario.eliminado" en RabbitMQ
     → microservicio-eventos recibe evento
     → Elimina todos los préstamos del usuario
```

### Evento 3: Préstamo Devuelto
```
[Usuario devuelve libro] → funcion-prestamos
  → UPDATE estado = 'DEVUELTO' en BD
  → Publica "prestamo.devuelto" en RabbitMQ
     → microservicio-eventos recibe evento
     → Incrementa cantidad_disponible del libro en 1
```

---

## RabbitMQ Management UI

URL: `http://localhost:15672`  
Usuario: `guest` | Contraseña: `guest`

Colas creadas automáticamente:
- `cola.prestamo.creado`
- `cola.prestamo.devuelto`
- `cola.usuario.eliminado`
- `cola.usuario.creado`

---

## Body de ejemplo para Postman

### Crear Usuario (POST /api/usuarios)
```json
{
  "nombre": "Juan",
  "apellido": "Perez",
  "email": "juan.perez@test.cl",
  "rut": "12345678-9",
  "telefono": "+56912345678"
}
```

### Crear Préstamo (POST /api/prestamos)
```json
{
  "idUsuario": 1,
  "idLibro": 1,
  "fechaDevolucionEsperada": "2026-06-30"
}
```
