package com.biblioteca.usuarios.graphql;

import com.biblioteca.usuarios.dao.UsuarioDAO;
import com.biblioteca.usuarios.events.EventPublisher;
import com.biblioteca.usuarios.model.Usuario;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.HashMap;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

/**
 * Motor GraphQL para la función de usuarios.
 * Define el esquema SDL y conecta los resolvers (DataFetchers) al DAO.
 */
public class UsuariosGraphQL {

    private static final String SCHEMA_SDL = """
            type Query {
                usuarios: [Usuario]
                usuario(id: ID!): Usuario
            }
            
            type Mutation {
                crearUsuario(
                    nombre: String!
                    apellido: String!
                    email: String!
                    rut: String!
                    telefono: String
                    estado: String
                ): Usuario
                
                actualizarUsuario(
                    id: ID!
                    nombre: String
                    apellido: String
                    email: String
                    rut: String
                    telefono: String
                    estado: String
                ): Usuario
                
                eliminarUsuario(id: ID!): Boolean
            }
            
            type Usuario {
                id: ID
                nombre: String
                apellido: String
                email: String
                rut: String
                telefono: String
                fechaRegistro: String
                estado: String
            }
            """;

    private final GraphQL graphQL;
    private final UsuarioDAO usuarioDAO;
    private final EventPublisher eventPublisher;

    public UsuariosGraphQL(UsuarioDAO usuarioDAO, EventPublisher eventPublisher) {
        this.usuarioDAO = usuarioDAO;
        this.eventPublisher = eventPublisher;

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(SCHEMA_SDL);
        RuntimeWiring wiring = construirWiring();
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
        this.graphQL = GraphQL.newGraphQL(schema).build();
    }

    private RuntimeWiring construirWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("usuarios", env -> usuarioDAO.listarTodos())
                        .dataFetcher("usuario", this::fetchUsuario))
                .type(newTypeWiring("Mutation")
                        .dataFetcher("crearUsuario", this::mutCrearUsuario)
                        .dataFetcher("actualizarUsuario", this::mutActualizarUsuario)
                        .dataFetcher("eliminarUsuario", this::mutEliminarUsuario))
                .build();
    }

    private Usuario fetchUsuario(DataFetchingEnvironment env) throws Exception {
        Long id = Long.parseLong(env.getArgument("id").toString());
        return usuarioDAO.obtenerPorId(id);
    }

    private Usuario mutCrearUsuario(DataFetchingEnvironment env) throws Exception {
        Usuario u = new Usuario();
        u.setNombre(env.getArgument("nombre"));
        u.setApellido(env.getArgument("apellido"));
        u.setEmail(env.getArgument("email"));
        u.setRut(env.getArgument("rut"));
        u.setTelefono(env.getArgument("telefono"));
        String estado = env.getArgument("estado");
        u.setEstado(estado != null ? estado : "ACTIVO");

        Usuario creado = usuarioDAO.crear(u);
        eventPublisher.publicarUsuarioCreado(creado.getId(), creado.getEmail());
        return creado;
    }

    private Usuario mutActualizarUsuario(DataFetchingEnvironment env) throws Exception {
        Long id = Long.parseLong(env.getArgument("id").toString());
        Usuario u = new Usuario();
        u.setNombre(env.getArgument("nombre"));
        u.setApellido(env.getArgument("apellido"));
        u.setEmail(env.getArgument("email"));
        u.setRut(env.getArgument("rut"));
        u.setTelefono(env.getArgument("telefono"));
        u.setEstado(env.getArgument("estado"));
        return usuarioDAO.actualizar(id, u);
    }

    private Boolean mutEliminarUsuario(DataFetchingEnvironment env) throws Exception {
        Long id = Long.parseLong(env.getArgument("id").toString());
        boolean eliminado = usuarioDAO.eliminar(id);
        if (eliminado) {
            eventPublisher.publicarUsuarioEliminado(id);
        }
        return eliminado;
    }

    /**
     * Ejecuta una consulta o mutación GraphQL y retorna el resultado como Map.
     */
    public Map<String, Object> ejecutar(String query, String operationName, Map<String, Object> variables) {
        ExecutionInput.Builder inputBuilder = ExecutionInput.newExecutionInput()
                .query(query);

        if (operationName != null && !operationName.isEmpty()) {
            inputBuilder.operationName(operationName);
        }
        if (variables != null && !variables.isEmpty()) {
            inputBuilder.variables(variables);
        }

        ExecutionResult result = graphQL.execute(inputBuilder.build());
        Map<String, Object> response = new HashMap<>();

        if (!result.getErrors().isEmpty()) {
            response.put("errors", result.getErrors());
        }
        response.put("data", result.getData());
        return response;
    }
}
