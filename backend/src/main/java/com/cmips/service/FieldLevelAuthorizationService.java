package com.cmips.service;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Field-Level Authorization Service
 * 
 * This service provides automatic field filtering based on Keycloak resource attributes.
 * It reads the 'allowed_fields' attribute from Keycloak resources to determine which
 * fields the current user can see for each resource.
 * 
 * This enables zero-code field-level authorization - all field access control is
 * configured in Keycloak through resource attributes.
 */
@Service
public class FieldLevelAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(FieldLevelAuthorizationService.class);

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    /**
     * Filters a map based on the allowed fields for the current user and resource (backward compatibility)
     * 
     * @param data The data to filter
     * @param resourceName The resource name to check permissions for
     * @return Filtered map containing only allowed fields
     */
    public Map<String, Object> filterFields(Map<String, Object> data, String resourceName) {
        return filterFields(data, resourceName, "read");
    }
    
    /**
     * Filters a map based on the allowed fields for the current user, resource, and scope
     * 
     * @param data The data to filter
     * @param resourceName The resource name to check permissions for
     * @param scope The scope (read, write, delete)
     * @return Filtered map containing only allowed fields
     */
    public Map<String, Object> filterFields(Map<String, Object> data, String resourceName, String scope) {
        try {
            Set<String> allowedFields = getAllowedFields(resourceName, scope);
            
            if (allowedFields.isEmpty()) {
                logger.warn("No allowed fields found for resource: {} with scope: {} - returning ALL fields (no field-level security configured)", resourceName, scope);
                // Return all data when no field-level security is configured
                return new HashMap<>(data);
            }
            
            // Filter the data to include only allowed fields
            Map<String, Object> filteredData = data.entrySet().stream()
                .filter(entry -> allowedFields.contains(entry.getKey()) && entry.getKey() != null)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue() != null ? entry.getValue() : ""
                ));
            
            logger.debug("Filtered {} fields to {} allowed fields for resource: {} with scope: {}", 
                data.size(), filteredData.size(), resourceName, scope);
            
            return filteredData;

        } catch (Exception e) {
            logger.error("Error filtering fields for resource: {} with scope: {} - {}", resourceName, scope, e.getMessage(), e);
            // Fail securely - return empty data if filtering fails
            return new HashMap<>();
        }
    }

    /**
     * Filters a list of maps based on the allowed fields for the current user and resource (backward compatibility)
     * 
     * @param dataList The list of data to filter
     * @param resourceName The resource name to check permissions for
     * @return Filtered list containing only allowed fields for each item
     */
    public List<Map<String, Object>> filterFields(List<Map<String, Object>> dataList, String resourceName) {
        return filterFields(dataList, resourceName, "read");
    }
    
    /**
     * Filters a list of maps based on the allowed fields for the current user, resource, and scope
     * 
     * @param dataList The list of data to filter
     * @param resourceName The resource name to check permissions for
     * @param scope The scope (read, write, delete)
     * @return Filtered list containing only allowed fields for each item
     */
    public List<Map<String, Object>> filterFields(List<Map<String, Object>> dataList, String resourceName, String scope) {
        try {
            Set<String> allowedFields = getAllowedFields(resourceName, scope);
            
            if (allowedFields.isEmpty()) {
                logger.warn("No allowed fields found for resource: {} with scope: {} - returning ALL items (no field-level security configured)", resourceName, scope);
                // Return all data when no field-level security is configured - convert to list of maps
                return dataList.stream()
                    .map(item -> new HashMap<>(item))
                    .collect(Collectors.toList());
            }
            
            // Filter each item in the list
            List<Map<String, Object>> filteredList = dataList.stream()
                .map(item -> filterFields(item, resourceName, scope))
                .collect(Collectors.toList());
            
            logger.debug("Filtered {} items for resource: {} with scope: {}", filteredList.size(), resourceName, scope);
            
            return filteredList;
            
        } catch (Exception e) {
            logger.error("Error filtering fields list for resource: {} with scope: {} - {}", resourceName, scope, e.getMessage(), e);
            // Fail securely - return empty list if filtering fails
            return new ArrayList<>();
        }
    }

    /**
     * Gets the allowed fields for the current user and resource from Keycloak (backward compatibility)
     * 
     * @param resourceName The resource name
     * @return Set of allowed field names
     */
    public Set<String> getAllowedFields(String resourceName) {
        return getAllowedFields(resourceName, "read");
    }
    
    /**
     * Gets the allowed fields for the current user, resource, and scope from Keycloak
     * 
     * @param resourceName The resource name
     * @param scope The scope (read, write, delete)
     * @return Set of allowed field names
     */
    public Set<String> getAllowedFields(String resourceName, String scope) {
        logger.info("Getting allowed fields for resource: {} with scope: {}", resourceName, scope);
        
        try {
            // 1. Get current user's role from JWT token
            String userRole = getCurrentUserRole();
            logger.info("Current user role: {}", userRole);
            
            // 2. Query Keycloak for resource attributes
            ResourceRepresentation resource = queryKeycloakForResourceAttributes(resourceName);
            if (resource == null) {
                logger.warn("Resource '{}' not found in Keycloak", resourceName);
                return new HashSet<>();
            }
            
            Map<String, List<String>> attributes = resource.getAttributes();
            if (attributes == null) {
                logger.warn("No attributes found for resource '{}'", resourceName);
                return new HashSet<>();
            }
            
            // 3. Build attribute key based on user role and scope
            String attributeKey = userRole.toLowerCase() + "_" + scope + "_fields";
            logger.info("Looking for attribute key: {}", attributeKey);
            
            // 4. Get the allowed fields string
            List<String> allowedFieldsList = attributes.get(attributeKey);
            // 4. Handle both array and single value formats
            Set<String> allowedFields = new HashSet<>();
            
            if (allowedFieldsList != null && !allowedFieldsList.isEmpty()) {
                // Keycloak now stores attributes as comma-separated strings
                for (Object fieldObj : allowedFieldsList) {
                    if (fieldObj instanceof String) {
                        String fieldStr = (String) fieldObj;
                        // Split comma-separated values and add each field
                        Set<String> fields = Arrays.stream(fieldStr.split(","))
                            .map(String::trim)
                            .filter(field -> !field.isEmpty())
                            .collect(Collectors.toSet());
                        allowedFields.addAll(fields);
                    }
                }
                
                logger.info("Raw allowed fields list for role {} with scope {}: {}", userRole, scope, allowedFieldsList);
                logger.info("Parsed allowed fields for role {} with scope {} in resource {}: {}", userRole, scope, resourceName, allowedFields);
            } else {
                logger.warn("No allowed fields found for role: {} with scope: {} in resource: {}", userRole, scope, resourceName);
            }
            
            return allowedFields;

        } catch (Exception e) {
            logger.error("Error getting allowed fields for resource: {} with scope: {} - {}", resourceName, scope, e.getMessage(), e);
            return new HashSet<>();
        }
    }
    
    /**
     * Gets the current user's role from the JWT token
     * 
     * @return The user's primary business role (PROVIDER, RECIPIENT, CASE_WORKER, or UNKNOWN)
     */
    private String getCurrentUserRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess != null && realmAccess.containsKey("roles")) {
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) realmAccess.get("roles");
                    logger.debug("User roles from JWT: {}", rolesList);
                    
                    // Find the first business role (not system roles)
                    for (String role : rolesList) {
                        if (!role.startsWith("default-roles") &&
                            !role.equals("uma_authorization") &&
                            !role.equals("offline_access") &&
                            (role.equals("PROVIDER") || role.equals("RECIPIENT") || role.equals("CASE_WORKER") || role.equals("SUPERVISOR"))) {
                            return role;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting user role from JWT: {}", e.getMessage(), e);
        }
        return "UNKNOWN";
    }
    
    // Cache for resource attributes to avoid repeated Keycloak calls
    private final Map<String, ResourceRepresentation> resourceCache = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile long cacheExpiry = 0;
    private static final long CACHE_TTL_MS = 300000; // 5 minutes

    /**
     * Queries Keycloak for resource attributes using Admin REST API
     *
     * @param resourceName The name of the resource to query
     * @return ResourceRepresentation with attributes, or null if not found
     */
    private ResourceRepresentation queryKeycloakForResourceAttributes(String resourceName) {
        try {
            // Check cache first
            long now = System.currentTimeMillis();
            if (now < cacheExpiry && resourceCache.containsKey(resourceName)) {
                logger.debug("Returning cached resource: {}", resourceName);
                return resourceCache.get(resourceName);
            }

            // Get admin token using client credentials
            String adminToken = getClientCredentialsToken();
            if (adminToken == null) {
                logger.error("Failed to get admin token");
                return null;
            }

            // Get client UUID
            String clientUuid = getClientUuid(adminToken);
            if (clientUuid == null) {
                logger.error("Failed to get client UUID for {}", clientId);
                return null;
            }

            // Query resources from Keycloak Admin API
            String resourcesUrl = keycloakServerUrl + "admin/realms/" + realm + "/clients/" + clientUuid + "/authz/resource-server/resource";
            logger.debug("Querying resources from: {}", resourcesUrl);

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(resourcesUrl))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Failed to get resources from Keycloak: {} - {}", response.statusCode(), response.body());
                return null;
            }

            // Parse JSON response
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> resources = mapper.readValue(response.body(),
                mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            // Update cache
            resourceCache.clear();
            cacheExpiry = now + CACHE_TTL_MS;

            for (Map<String, Object> resourceMap : resources) {
                ResourceRepresentation resource = new ResourceRepresentation();
                resource.setName((String) resourceMap.get("name"));
                resource.setId((String) resourceMap.get("_id"));
                resource.setType((String) resourceMap.get("type"));

                @SuppressWarnings("unchecked")
                Map<String, List<String>> attributes = (Map<String, List<String>>) resourceMap.get("attributes");
                if (attributes != null) {
                    resource.setAttributes(attributes);
                }

                resourceCache.put(resource.getName(), resource);
                logger.debug("Cached resource: {} with {} attributes", resource.getName(),
                    attributes != null ? attributes.size() : 0);
            }

            ResourceRepresentation result = resourceCache.get(resourceName);
            if (result != null) {
                logger.info("Found resource: {} with attributes: {}", resourceName, result.getAttributes());
            } else {
                logger.warn("Resource '{}' not found. Available: {}", resourceName, resourceCache.keySet());
            }
            return result;

        } catch (Exception e) {
            logger.error("Error querying Keycloak for resource attributes: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets a client credentials token for admin API access
     */
    private String getClientCredentialsToken() {
        try {
            String tokenUrl = keycloakServerUrl + "realms/" + realm + "/protocol/openid-connect/token";

            String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> tokenResponse = mapper.readValue(response.body(), Map.class);
                return (String) tokenResponse.get("access_token");
            } else {
                logger.error("Failed to get client credentials token: {} - {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error getting client credentials token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets the client UUID from Keycloak
     */
    private String getClientUuid(String adminToken) {
        try {
            String clientsUrl = keycloakServerUrl + "admin/realms/" + realm + "/clients?clientId=" + clientId;

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(clientsUrl))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, Object>> clients = mapper.readValue(response.body(),
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                if (!clients.isEmpty()) {
                    return (String) clients.get(0).get("id");
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting client UUID: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Checks if the current user has access to a specific field for a resource
     * 
     * @param resourceName The resource name
     * @param fieldName The field name to check
     * @return true if the user can access this field
     */
    public boolean canAccessField(String resourceName, String fieldName) {
        Set<String> allowedFields = getAllowedFields(resourceName);
        return allowedFields.contains(fieldName);
    }


    /**
     * Filters request fields to only include fields the user can write to
     * 
     * @param requestData The request data to filter
     * @param resourceName The resource name
     * @return Filtered request data containing only writable fields
     */
    public Map<String, Object> filterRequestFields(Map<String, Object> requestData, String resourceName) {
        return filterFields(requestData, resourceName, "write");
    }
    
    /**
     * Gets the allowed actions for the current user and resource from Keycloak
     * 
     * @param resourceName The resource name
     * @return Set of allowed action names (e.g., "edit", "delete", "approve")
     */
    public Set<String> getAllowedActions(String resourceName) {
        logger.info("Getting allowed actions for resource: {}", resourceName);
        
        try {
            // 1. Get current user's role from JWT token
            String userRole = getCurrentUserRole();
            logger.info("Current user role: {}", userRole);
            
            // 2. Query Keycloak for resource attributes
            ResourceRepresentation resource = queryKeycloakForResourceAttributes(resourceName);
            if (resource == null) {
                logger.warn("Resource '{}' not found in Keycloak", resourceName);
                return new HashSet<>();
            }
            
            Map<String, List<String>> attributes = resource.getAttributes();
            if (attributes == null) {
                logger.warn("No attributes found for resource '{}'", resourceName);
                return new HashSet<>();
            }
            
            // 3. Build attribute key based on user role
            String attributeKey = userRole.toLowerCase() + "_actions";
            logger.info("Looking for attribute key: {}", attributeKey);
            
            // 4. Get the allowed actions string
            List<String> allowedActionsList = attributes.get(attributeKey);
            if (allowedActionsList == null || allowedActionsList.isEmpty()) {
                logger.warn("No allowed actions found for role: {} in resource: {}", userRole, resourceName);
                return new HashSet<>();
            }
            
            String allowedActionsStr = allowedActionsList.get(0);
            logger.info("Raw allowed actions string for role {}: {}", userRole, allowedActionsStr);
            
            // 5. Parse comma-separated actions
            Set<String> allowedActions = Arrays.stream(allowedActionsStr.split(","))
                .map(String::trim)
                .filter(action -> !action.isEmpty())
                .collect(Collectors.toSet());
            
            logger.info("Parsed allowed actions for role {} in resource {}: {}", userRole, resourceName, allowedActions);
            return allowedActions;

        } catch (Exception e) {
            logger.error("Error getting allowed actions for resource: {} - {}", resourceName, e.getMessage(), e);
            return new HashSet<>();
        }
    }
    
    /**
     * Checks if the current user can perform a specific action on a resource
     * 
     * @param resourceName The resource name
     * @param action The action to check (e.g., "edit", "delete")
     * @return true if the user can perform this action
     */
    public boolean canPerformAction(String resourceName, String action) {
        Set<String> allowedActions = getAllowedActions(resourceName);
        return allowedActions.contains(action);
    }
    
    /**
     * Gets a human-readable description of field access for debugging
     * 
     * @param resourceName The resource name
     * @return Description of field access
     */
    public String getFieldAccessDescription(String resourceName) {
        Set<String> allowedFields = getAllowedFields(resourceName);
        
        if (allowedFields.isEmpty()) {
            return "No field access for resource: " + resourceName;
        }
        
        return String.format("Allowed fields for %s: %s", 
            resourceName, 
            String.join(", ", allowedFields));
    }
    
    /**
     * Gets a human-readable description of field access for a specific scope
     *
     * @param resourceName The resource name
     * @param scope The scope (read, write, delete)
     * @return Description of field access
     */
    public String getFieldAccessDescription(String resourceName, String scope) {
        Set<String> allowedFields = getAllowedFields(resourceName, scope);

        if (allowedFields.isEmpty()) {
            return String.format("No %s field access for resource: %s", scope, resourceName);
        }

        return String.format("Allowed %s fields for %s: %s",
            scope, resourceName, String.join(", ", allowedFields));
    }

    /**
     * Filters fields for an entity based on user role and resource permissions.
     * Converts the entity to a map and filters based on allowed fields.
     *
     * @param entity The entity object to filter
     * @param userRoles The user's roles (comma-separated string or single role)
     * @param resourceName The resource name to check permissions for
     * @return Map containing only allowed fields
     */
    public Map<String, Object> filterFieldsForRole(Object entity, String userRoles, String resourceName) {
        if (entity == null) {
            return new HashMap<>();
        }

        try {
            // Convert entity to map using reflection
            Map<String, Object> entityMap = convertEntityToMap(entity);

            // Use the existing filterFields method with the entity map
            return filterFields(entityMap, resourceName, "read");
        } catch (Exception e) {
            logger.error("Error filtering fields for entity of type {} with resource {}: {}",
                entity.getClass().getSimpleName(), resourceName, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Converts an entity object to a Map using reflection
     *
     * @param entity The entity to convert
     * @return Map representation of the entity
     */
    private Map<String, Object> convertEntityToMap(Object entity) {
        Map<String, Object> map = new HashMap<>();

        try {
            Class<?> clazz = entity.getClass();

            // Get all declared fields including superclass fields
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = field.get(entity);
                map.put(fieldName, value);
            }

            // Also include superclass fields
            Class<?> superclass = clazz.getSuperclass();
            while (superclass != null && superclass != Object.class) {
                for (java.lang.reflect.Field field : superclass.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    if (!map.containsKey(fieldName)) {
                        Object value = field.get(entity);
                        map.put(fieldName, value);
                    }
                }
                superclass = superclass.getSuperclass();
            }
        } catch (Exception e) {
            logger.error("Error converting entity to map: {}", e.getMessage(), e);
        }

        return map;
    }
}