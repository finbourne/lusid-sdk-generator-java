package com.finbourne.drive.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.nio.charset.Charset;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@WireMockTest
public class WiremockTests {

    @Test
    public void verifyAllEndpointsTest(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        String swaggerFilePath = "/Users/charlie/repos/lusid-sdk-generator-java/generate/.output/sdk/swagger.json";
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        OpenApiSpecification spec = mapper.readValue(new File(swaggerFilePath), OpenApiSpecification.class);
        List<String> failures = new ArrayList<String>();
        for (String path : spec.paths.keySet()) {
            for (String operation : spec.paths.get(path).keySet()) {

                WireMock.resetAllRequests();
                WireMock.removeAllMappings();

                OpenApiOperation operationData = spec.paths.get(path).get(operation);

                // get the expected response
                String firstSuccessfulStatusCodeString = operationData.responses
                    .keySet()
                    .stream()
                    .filter(x -> x != "default" && Integer.parseInt(x) >= 200 && Integer.parseInt(x) < 300)
                    .findFirst()
                    .get();
                int firstSuccessfulStatusCode = Integer.parseInt(firstSuccessfulStatusCodeString);
                OpenApiResponse response = operationData.responses.get(firstSuccessfulStatusCodeString);
                Object responseExample = null;
                if (response.content != null) {
                    OpenApiContent firstResponse = response.content.values().iterator().next();
                    if (firstResponse.example != null) {
                        responseExample = firstResponse.example;
                    } else if (firstResponse.examples != null && firstResponse.examples.size() > 0) {
                        responseExample = firstResponse.examples.values().iterator().next();
                    } else {
                        responseExample = getObjectFromSchema(firstResponse.schema);
                    }
                    String responseJson = mapper.writeValueAsString(responseExample);
                    stubFor(any(urlPathTemplate(path))
                        .willReturn(new ResponseDefinitionBuilder()
                            .withStatus(firstSuccessfulStatusCode)
                            .withBody(responseJson)));
                } else {
                    stubFor(any(urlPathTemplate(path))
                        .willReturn(new ResponseDefinitionBuilder()
                            .withStatus(firstSuccessfulStatusCode)));
                }

                Object requestBody = null;
                // get the request body if there is one
                if (operationData.requestBody != null) {
                    OpenApiContent firstOperation = operationData.requestBody.content.values().iterator().next();
                    Class<?> requestBodyClass = null;
                    if (firstOperation.schema.ref != null) {
                        String typeName = getTypeNameFromSchemaRef(firstOperation.schema.ref);
                        String fullyQualifiedName = "com.finbourne.drive.model." + typeName;
                        requestBodyClass = Class.forName(fullyQualifiedName);
                    }
                    if (firstOperation.example != null) {
                        if (requestBodyClass != null) {
                            requestBody = mapper.convertValue(firstOperation.example, requestBodyClass);
                        } else {
                            requestBody = firstOperation.example;
                        }
                    } else if (firstOperation.examples != null && firstOperation.examples.size() > 0) {
                        Object requestBodyObj = firstOperation.examples.values().iterator().next();
                        if (requestBodyClass != null) {
                            requestBody = mapper.convertValue(requestBodyObj, requestBodyClass);
                        } else {
                            requestBody = requestBodyObj;
                        }
                    } else {
                        requestBody = getObjectFromSchema(firstOperation.schema);
                        Class<?> requestBodyType = requestBody.getClass();
                        System.out.println(requestBodyType);
                    }
                }
                
                // get the arguments
                HashMap<String, ParameterValue> parameters = new HashMap<>();
                if (operationData.parameters != null) {
                    for (OpenApiParameter parameter : operationData.parameters) {
                        Object argValue = getObjectFromSchema(parameter.schema);
                        parameters.put(parameter.name, new ParameterValue(parameter.in, argValue));
                    }
                }

                List<Object> args = getArgumentsOfKind(parameters, "path", "header");
                if (requestBody != null) {
                    args.add(requestBody);
                }

                try {
                    // get the method to invoke
                    String controller = operationData.tags[0].replaceAll(" ", "");
                    String fullyQualifiedName = "com.finbourne.drive.api." + controller + "Api";
                    String methodName = lowercaseFirstLetter(operationData.operationId);
                    Class<?> clazz = Class.forName(fullyQualifiedName);
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    Method setCustomBaseUrlMethod = getMethod(clazz, "setCustomBaseUrl", false);
                    setCustomBaseUrlMethod.invoke(instance, wmRuntimeInfo.getHttpBaseUrl());
                    Method method = getMethod(clazz, methodName, false);
                    Object[] argsArray = args.toArray();
                    Object resultToExecute = method.invoke(instance, argsArray);
                    Class<?> resultsToExecuteClass = resultToExecute.getClass();
                    // set the query parameters
                    for (String parameter : parameters.keySet()) {
                        ParameterValue paramValue = parameters.get(parameter);
                        if (paramValue.kind.equals("query")) {
                            Method setQueryArgMethod = getMethod(resultsToExecuteClass, parameter, false);
                            try {
                                setQueryArgMethod.invoke(resultToExecute, paramValue.value);
                            } catch (Exception e) {
                                throw e;
                            }
                        }
                    }
                    Method executeMethod = getMethod(resultToExecute.getClass(), "execute", true);
                    Object result = executeMethod.invoke(resultToExecute);
                    System.out.println("- " + operationData.operationId + " succeeded");
                } catch (Exception e) {
                    e.printStackTrace();
                    failures.add(e.getMessage());
                    System.out.println("- " + operationData.operationId + " failed");
                    System.out.println(e.getMessage());
                }
            }
        }
        assertThat(failures.size(), is(0));
    }

    private static List<Object> getArgumentsOfKind(HashMap<String, ParameterValue> parameters, String... kinds) {
        return parameters.values().stream().filter(x -> Arrays.stream(kinds).anyMatch(x.kind::equals)).map(x -> x.value).collect(Collectors.toList());
    }

    private static Method getMethod(Class<?> clazz, String methodName, Boolean ensureNoParameters) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(x -> x.getName().equals(methodName) && (!ensureNoParameters || x.getParameterCount() == 0))
            .findFirst()
            .get();
    }

    private static String lowercaseFirstLetter(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private static String getTypeNameFromSchemaRef(String schemaRef) {
        String[] parts = schemaRef.split("/");
        return parts[parts.length - 1];
    }

    private static Object getObjectFromSchema(OpenApiSchema schema) throws Exception {

        if (schema.ref != null) {
            String typeName = getTypeNameFromSchemaRef(schema.ref);
            PodamFactory factory = new PodamFactoryImpl();
            Class<?> clazz = Class.forName("com.finbourne.drive.model." + typeName);
            Object obj = factory.manufacturePojoWithFullData(clazz);
            return obj;
        }

        int minLength = schema.minLength == null ? 1 : schema.minLength;
        int maxLength = schema.maxLength == null ? 10 : schema.maxLength;
        if (maxLength > 100) {
            maxLength = 100;
        }

        if (schema.format != null && (schema.format.equals("byte") || schema.format.equals("binary"))) {
            return getRandomString(minLength, maxLength).getBytes(Charset.forName("UTF-8"));
        }

        if (schema.type.equals("string")) {
            return getRandomString(minLength, maxLength);
        }

        if (schema.type.equals("integer") && schema.format.equals("int32")) {
            int minValue = (int) Math.pow(10, minLength - 1);
            int maxValue = (int) Math.pow(10, maxLength) - 1;
            return ThreadLocalRandom.current().nextInt(minValue, maxValue + 1);
        }

        if (schema.type.equals("integer") && schema.format.equals("int64")) {
            long minValue = (long) Math.pow(10, minLength - 1);
            long maxValue = (long) Math.pow(10, maxLength) - 1;
            return ThreadLocalRandom.current().nextLong(minValue, maxValue + 1);
        }

        if (schema.type.equals("boolean")) {
            return true;
        }

        if (schema.type.equals("array") && schema.items.type.equals("string")) {
            int arrayLength = ThreadLocalRandom.current().nextInt(minLength, maxLength + 1);
            String[] array = new String[arrayLength];

            // Fill the array with random strings
            for (int i = 0; i < arrayLength; i++) {
                array[i] = UUID.randomUUID().toString();
            }
            return Arrays.asList(array);
        }

        throw new Exception("unhandled case for schema with type: '" + schema.type + "' and format: '" + schema.format + "'");
    }

    private static String getRandomString(int minLength, int maxLength) {
        int stringLength = ThreadLocalRandom.current().nextInt(minLength, maxLength + 1);
        StringBuilder sb = new StringBuilder(stringLength);
        Random random = new Random();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < stringLength; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        return sb.toString();
    }
}

class ParameterValue {
    public ParameterValue(String kind, Object value) {
        this.kind = kind;
        this.value = value;
    }
    public String kind;
    public Object value;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenApiResponse {
    public HashMap<String, OpenApiContent> content;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenApiRequestBody {
    public HashMap<String, OpenApiContent> content;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenApiContent {
    public OpenApiSchema schema;
    public Object example;
    public HashMap<String, Object> examples;
}

class OpenApiSchemaArray {
    public String type;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenApiSchema {
    public String type;
    public OpenApiSchemaArray items;
    public String format;
    public Integer maxLength;
    public Integer minLength;
    @JsonProperty("$ref")
    public String ref;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenApiParameter {
    public String name;
    public String in;
    public OpenApiSchema schema;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenApiOperation {
    public String[] tags;
    public String operationId;
    public OpenApiParameter[] parameters;
    public OpenApiRequestBody requestBody;
    public HashMap<String, OpenApiResponse> responses;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class OpenApiSpecification {
    public HashMap<String, HashMap<String, OpenApiOperation>> paths;
}
