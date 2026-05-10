package com.biblioteca.prestamos.graphql;

import com.biblioteca.prestamos.dao.PrestamoDAO;
import com.biblioteca.prestamos.events.EventPublisher;
import com.biblioteca.prestamos.model.Prestamo;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

/**
 * Motor GraphQL para la función de préstamos.
 * Los resolvers llaman directamente al PrestamoDAO.
 */
public class PrestamosGraphQL {

    private static final String SCHEMA_SDL = """
            type Query {
                prestamos: [Prestamo]
                prestamo(id: ID!): Prestamo
                prestamosPorUsuario(idUsuario: ID!): [Prestamo]
            }
            
            type Mutation {
                crearPrestamo(
                    idUsuario: ID!
                    idLibro: ID!
                    fechaDevolucionEsperada: String!
                ): Prestamo
                
                actualizarPrestamo(
                    id: ID!
                    idUsuario: ID
                    idLibro: ID
                    fechaDevolucionEsperada: String
                    estado: String
                ): Prestamo
                
                devolverPrestamo(id: ID!): Prestamo
                eliminarPrestamo(id: ID!): Boolean
            }
            
            type Prestamo {
                id: ID
                idUsuario: ID
                idLibro: ID
                fechaPrestamo: String
                fechaDevolucionEsperada: String
                fechaDevolucionReal: String
                estado: String
            }
            """;

    private final GraphQL graphQL;
    private final PrestamoDAO prestamoDAO;
    private final EventPublisher eventPublisher;

    public PrestamosGraphQL(PrestamoDAO prestamoDAO, EventPublisher eventPublisher) {
        this.prestamoDAO = prestamoDAO;
        this.eventPublisher = eventPublisher;

        TypeDefinitionRegistry registry = new SchemaParser().parse(SCHEMA_SDL);
        RuntimeWiring wiring = construirWiring();
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        this.graphQL = GraphQL.newGraphQL(schema).build();
    }

    private RuntimeWiring construirWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("prestamos", env -> prestamoDAO.listarTodos())
                        .dataFetcher("prestamo", this::fetchPrestamo)
                        .dataFetcher("prestamosPorUsuario", this::fetchPorUsuario))
                .type(newTypeWiring("Mutation")
                        .dataFetcher("crearPrestamo", this::mutCrear)
                        .dataFetcher("actualizarPrestamo", this::mutActualizar)
                        .dataFetcher("devolverPrestamo", this::mutDevolver)
                        .dataFetcher("eliminarPrestamo", this::mutEliminar))
                .build();
    }

    private Prestamo fetchPrestamo(DataFetchingEnvironment env) throws Exception {
        Long id = Long.parseLong(env.getArgument("id").toString());
        return prestamoDAO.obtenerPorId(id);
    }

    private Object fetchPorUsuario(DataFetchingEnvironment env) throws Exception {
        Long idUsuario = Long.parseLong(env.getArgument("idUsuario").toString());
        return prestamoDAO.listarPorUsuario(idUsuario);
    }

    private Prestamo mutCrear(DataFetchingEnvironment env) throws Exception {
        Prestamo p = new Prestamo();
        p.setIdUsuario(Long.parseLong(env.getArgument("idUsuario").toString()));
        p.setIdLibro(Long.parseLong(env.getArgument("idLibro").toString()));
        p.setFechaDevolucionEsperada(Date.valueOf(env.getArgument("fechaDevolucionEsperada").toString()));

        Prestamo creado = prestamoDAO.crear(p);
        // Publica evento: microservicio-eventos actualizará disponibilidad del libro
        eventPublisher.publicarPrestamoCreado(creado.getId(), creado.getIdUsuario(), creado.getIdLibro());
        return creado;
    }

    private Prestamo mutActualizar(DataFetchingEnvironment env) throws Exception {
        Long id = Long.parseLong(env.getArgument("id").toString());
        Prestamo existente = prestamoDAO.obtenerPorId(id);
        if (existente == null) return null;

        if (env.getArgument("idUsuario") != null)
            existente.setIdUsuario(Long.parseLong(env.getArgument("idUsuario").toString()));
        if (env.getArgument("idLibro") != null)
            existente.setIdLibro(Long.parseLong(env.getArgument("idLibro").toString()));
        if (env.getArgument("fechaDevolucionEsperada") != null)
            existente.setFechaDevolucionEsperada(Date.valueOf(env.getArgument("fechaDevolucionEsperada").toString()));
        if (env.getArgument("estado") != null)
            existente.setEstado(env.getArgument("estado"));

        return prestamoDAO.actualizar(id, existente);
    }

    private Prestamo mutDevolver(DataFetchingEnvironment env) throws Exception {
        Long id = Long.parseLong(env.getArgument("id").toString());
        Prestamo devuelto = prestamoDAO.devolverLibro(id);
        if (devuelto != null) {
            eventPublisher.publicarPrestamoDevuelto(devuelto.getId(), devuelto.getIdLibro());
        }
        return devuelto;
    }

    private Boolean mutEliminar(DataFetchingEnvironment env) throws Exception {
        Long id = Long.parseLong(env.getArgument("id").toString());
        return prestamoDAO.eliminar(id);
    }

    public Map<String, Object> ejecutar(String query, String operationName, Map<String, Object> variables) {
        ExecutionInput.Builder builder = ExecutionInput.newExecutionInput().query(query);
        if (operationName != null && !operationName.isEmpty()) builder.operationName(operationName);
        if (variables != null && !variables.isEmpty()) builder.variables(variables);

        ExecutionResult result = graphQL.execute(builder.build());
        Map<String, Object> response = new HashMap<>();
        if (!result.getErrors().isEmpty()) response.put("errors", result.getErrors());
        response.put("data", result.getData());
        return response;
    }
}
