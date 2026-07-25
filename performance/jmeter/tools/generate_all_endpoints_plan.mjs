import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const toolDirectory = path.dirname(fileURLToPath(import.meta.url));
const suiteRoot = path.resolve(toolDirectory, "..");
const output = path.join(suiteRoot, "plans", "vargasvet-all-endpoints-load.jmx");
const smokeOutput = path.join(suiteRoot, "plans", "vargasvet-all-endpoints-smoke.jmx");
const openApiUrl = process.env.VARGASVET_OPENAPI_URL ?? "http://127.0.0.1:8081/api/v1/v3/api-docs";

const xml = (value) => String(value ?? "")
  .replaceAll("&", "&amp;")
  .replaceAll("<", "&lt;")
  .replaceAll(">", "&gt;")
  .replaceAll('"', "&quot;");

const prop = (tag, name, value, indent = "") =>
  `${indent}<${tag} name="${xml(name)}">${xml(value)}</${tag}>`;

const variables = {
  protocol: "${__P(protocol,http)}",
  host: "${__P(host,127.0.0.1)}",
  port: "${__P(port,8081)}",
  base_path: "${__P(base_path,/api/v1)}",
  company_id: "${__P(company_id,1)}",
  user_id: "${__P(user_id,1)}",
  guardian_id: "${__P(guardian_id,1)}",
  pet_id: "${__P(pet_id,1)}",
  pet_uuid: "${__P(pet_uuid,00000000-0000-0000-0000-000000000001)}",
  employee_id: "${__P(employee_id,1)}",
  employee_type_id: "${__P(employee_type_id,1)}",
  service_id: "${__P(service_id,1)}",
  appointment_id: "${__P(appointment_id,1)}",
  consultation_id: "${__P(consultation_id,1)}",
  medical_record_id: "${__P(medical_record_id,1)}",
  medical_record_number: "${__P(medical_record_number,HC-1)}",
  payment_id: "${__P(payment_id,1)}",
  prescription_id: "${__P(prescription_id,1)}",
  role_id: "${__P(role_id,1)}",
  user_role_id: "${__P(user_role_id,1)}",
  schedule_id: "${__P(schedule_id,1)}",
  specialty_id: "${__P(specialty_id,1)}",
  window_id: "${__P(window_id,1)}",
  view_id: "${__P(view_id,1)}",
  file_id: "${__P(file_id,1)}",
  generic_id: "${__P(generic_id,1)}",
  mutation_id: "${__P(mutation_id,999999999)}",
  reset_token: "${__P(reset_token,load-test-invalid-token)}",
  media_filename: "${__P(media_filename,load-test.png)}",
  appointment_date: "${__P(appointment_date,2026-12-15)}",
  sample_file: "${__P(sample_file,performance/jmeter/config/sample-load.png)}",
  email: "${__P(load_email,)}",
  password: "${__P(load_password,)}",
  owner_email: "${__P(load_owner_email,)}",
  owner_password: "${__P(load_owner_password,)}",
  vet_email: "${__P(load_vet_email,)}",
  vet_password: "${__P(load_vet_password,)}",
};

function variableFor(name, route, method) {
  const normalized = name.toLowerCase();
  if (method !== "GET" && (normalized === "id" || normalized.endsWith("id"))) return "mutation_id";
  const direct = {
    companyid: "company_id", userid: "user_id", userid: "user_id", petid: "pet_id", mascotaid: "pet_id",
    employeeid: "employee_id", empleadoid: "employee_id", veterinarianid: "employee_id", veterinarioid: "employee_id",
    serviceid: "service_id", servicioid: "service_id", appointmentid: "appointment_id", citaid: "appointment_id",
    consultationid: "consultation_id", recordid: "medical_record_id", medicalrecordid: "medical_record_id",
    roleid: "role_id", useridrole: "user_role_id", userroleid: "user_role_id", guardianid: "guardian_id",
    scheduleid: "schedule_id", specialtyid: "specialty_id", especialidadid: "specialty_id",
    windowid: "window_id", viewid: "view_id", fileid: "file_id", prescriptionid: "prescription_id",
    filename: "media_filename", petuuid: "pet_uuid",
    numerohc: "medical_record_number", token: "reset_token", uuid: "pet_uuid",
  };
  if (direct[normalized]) return direct[normalized];
  if (normalized !== "id") return `${normalized.replace(/[^a-z0-9]+/g, "_")}_id`;
  if (route.startsWith("/services")) return "service_id";
  if (route.startsWith("/prescriptions")) return "prescription_id";
  if (route.startsWith("/pets")) return "pet_id";
  if (route.startsWith("/consultations")) return "consultation_id";
  if (route.startsWith("/appointments")) return "appointment_id";
  if (route.startsWith("/medical-records")) return "medical_record_id";
  if (route.startsWith("/clients/guardians")) return "guardian_id";
  if (route.startsWith("/admin/company")) return "company_id";
  if (route.startsWith("/admin/employee-types")) return "employee_type_id";
  if (route.startsWith("/admin/employees")) return "employee_id";
  if (route.startsWith("/admin/roles")) return "role_id";
  if (route.startsWith("/admin/specialties")) return "specialty_id";
  if (route.startsWith("/admin/windows")) return "window_id";
  if (route.startsWith("/admin/views")) return "view_id";
  if (route.includes("/files")) return "file_id";
  return "generic_id";
}

const futureDateTime = "${appointment_date}T15:00:00";
function knownValue(name, schema = {}) {
  const key = name.toLowerCase();
  if (key.includes("email")) return "load-${__threadNum}-${__time()}@vargasvet.test";
  if (key.includes("password")) return "LoadTest!12345";
  if (key === "companyid") return "${company_id}";
  if (["petid", "mascotaid"].includes(key)) return "${pet_id}";
  if (["employeeid", "empleadoid", "veterinarioid", "veterinarianid"].includes(key)) return "${employee_id}";
  if (["serviceid", "servicioid"].includes(key)) return "${service_id}";
  if (["appointmentid", "citaid"].includes(key)) return "${appointment_id}";
  if (key === "consultationid") return "${consultation_id}";
  if (key === "roleid") return "${role_id}";
  if ((key.includes("fecha") && key.includes("hora")) || key.includes("datetime")) return futureDateTime;
  if (key.includes("fecha") || key.includes("date") || key.includes("weekstart")) return "${appointment_date}";
  if (key.includes("hora")) return "15:00:00";
  if (key.includes("monto") || key.includes("precio")) return 100;
  if (key.includes("documento") || key.includes("dni")) return "7${__threadNum}${__time(SSS)}";
  if (key.includes("telefono") || key.includes("celular")) return "999${__time(HHmmss)}";
  if (key.includes("motivo")) return "Prueba de carga JMeter";
  if (key.includes("nombre")) return `Carga ${name} \${__threadNum}`;
  if (key.includes("apellido")) return "JMeter";
  if (schema.format === "date-time") return futureDateTime;
  if (schema.format === "date") return "${appointment_date}";
  if (schema.format === "time") return "15:00:00";
  return undefined;
}

function resolveSchema(schema, api) {
  if (!schema) return {};
  if (schema.$ref) {
    const name = schema.$ref.split("/").at(-1);
    return api.components?.schemas?.[name] ?? {};
  }
  return schema;
}

function sampleFor(schema, api, name = "value", depth = 0) {
  schema = resolveSchema(schema, api);
  if (schema.example !== undefined) return schema.example;
  if (schema.default !== undefined) return schema.default;
  const known = knownValue(name, schema);
  if (known !== undefined) return known;
  if (schema.enum?.length) return schema.enum[0];
  if (depth > 3) return null;
  if (schema.type === "array") return [sampleFor(schema.items, api, name, depth + 1)];
  if (schema.type === "boolean") return true;
  if (schema.type === "integer" || schema.type === "number") return Math.max(Number(schema.minimum ?? 1), 1);
  if (schema.type === "string") return schema.format === "uuid" ? "00000000-0000-0000-0000-000000000001" : `Carga ${name}`;
  const properties = schema.properties ?? {};
  const keys = schema.required?.length ? schema.required : Object.keys(properties);
  return Object.fromEntries(keys.map((key) => [key, sampleFor(properties[key], api, key, depth + 1)]));
}

function argumentsXml(body, formArguments = []) {
  return `<elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="">
            <collectionProp name="Arguments.arguments">${body === undefined ? "" : `
              <elementProp name="" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">false</boolProp>
                <stringProp name="Argument.value">${xml(JSON.stringify(body))}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>`}${formArguments.map(({ name, value }) => `
              <elementProp name="${xml(name)}" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">true</boolProp>
                <stringProp name="Argument.name">${xml(name)}</stringProp>
                <stringProp name="Argument.value">${xml(value)}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
                <boolProp name="HTTPArgument.use_equals">true</boolProp>
              </elementProp>`).join("")}
            </collectionProp>
          </elementProp>`;
}

function filesXml(parameterName = "file") {
  return `<elementProp name="HTTPsampler.Files" elementType="HTTPFileArgs">
            <collectionProp name="HTTPFileArgs.files">
              <elementProp name="${xml(parameterName)}" elementType="HTTPFileArg">
                ${prop("stringProp", "File.path", "${sample_file}", "                ")}
                ${prop("stringProp", "File.paramname", parameterName, "                ")}
                ${prop("stringProp", "File.mimetype", "image/png", "                ")}
              </elementProp>
            </collectionProp>
          </elementProp>`;
}

function assertionChildren(hasJsonBody, expectedPattern) {
  return `${hasJsonBody ? `<HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="JSON" enabled="true"><collectionProp name="HeaderManager.headers"><elementProp name="" elementType="Header"><stringProp name="Header.name">Content-Type</stringProp><stringProp name="Header.value">application/json</stringProp></elementProp></collectionProp></HeaderManager><hashTree />` : ""}
          <JSR223Assertion guiclass="TestBeanGUI" testclass="JSR223Assertion" testname="Código HTTP esperado" enabled="true">
            <stringProp name="cacheKey">true</stringProp><stringProp name="filename" /><stringProp name="parameters" />
            <stringProp name="script">def esperado = prev.getResponseCode() ==~ /${xml(expectedPattern)}/; prev.setSuccessful(esperado); if (!esperado) { def detalle = prev.getResponseDataAsString(); if (detalle.length() &gt; 400) detalle = detalle.substring(0, 400); AssertionResult.setFailure(true); AssertionResult.setFailureMessage('Código inesperado: ' + prev.getResponseCode() + ' | ' + detalle) }</stringProp>
            <stringProp name="scriptLanguage">groovy</stringProp>
          </JSR223Assertion><hashTree />`;
}

function expectedPatternFor(method, route) {
  if (route === "/media/{filename}" || route === "/consultations/{consultationId}/files/{id}/content" ||
      route === "/payments/appointment/{appointmentId}") {
    return "(?:2\\d\\d|404)";
  }
  const controlledNegative = route.startsWith("/auth/verify/") ||
    route === "/auth/validate-reset-token" || route === "/auth/reset-password" ||
    route === "/auth/setup-account" || route === "/auth/resend-verification" ||
    route === "/auth/forgot-password" || route === "/setup/first-admin";
  if (controlledNegative || method !== "GET") return "(?:2|4)\\d\\d";
  return "2\\d\\d";
}

function multipartValue(name, schema, api) {
  const resolved = resolveSchema(schema, api);
  if (resolved.enum?.length) return resolved.enum[0];
  const key = name.toLowerCase();
  if (key === "tipo") return "IMAGEN";
  if (key === "especie") return "Perro";
  if (key.includes("descripcion")) return "Archivo de prueba de carga";
  return knownValue(name, resolved) ?? resolved.default ?? "Carga JMeter";
}

function sampler(id, method, route, operation, api) {
  let requestPath = route.replaceAll(/\{([^}:]+)(?::[^}]+)?\}/g, (_, name) => `\${${variableFor(name, route, method)}}`);
  const query = [];
  for (const parameter of operation.parameters ?? []) {
    if (parameter.in !== "query") continue;
    const known = knownValue(parameter.name, parameter.schema);
    const isCompanyContext = parameter.name.toLowerCase() === "companyid";
    if (!parameter.required && parameter.schema?.default === undefined && !isCompanyContext) continue;
    const value = known ?? parameter.schema?.default ?? parameter.schema?.enum?.[0] ?? "1";
    query.push(`${encodeURIComponent(parameter.name)}=${value}`);
  }
  if (query.length) requestPath += `${requestPath.includes("?") ? "&" : "?"}${query.join("&")}`;

  const content = operation.requestBody?.content ?? {};
  const jsonSchema = content["application/json"]?.schema;
  const multipart = content["multipart/form-data"];
  const forceMultipartFile = route === "/media/upload";
  let body = jsonSchema && !forceMultipartFile ? sampleFor(jsonSchema, api) : undefined;
  if ((route === "/admin/employees" && method === "POST") ||
      (route === "/admin/employees/{id}" && method === "PUT") ||
      (route === "/admin/views" && method === "POST") ||
      (route === "/clients/guardians" && method === "POST") ||
      (route === "/clients/guardians/{id}" && method === "PUT")) {
    body = {};
  }
  const multipartProperties = multipart ? Object.entries(resolveSchema(multipart.schema, api).properties ?? {}) : [];
  const fileParameter = forceMultipartFile ? "file" : multipartProperties
    .find(([, value]) => resolveSchema(value, api).format === "binary")?.[0] ?? (multipart ? "file" : null);
  const formArguments = multipartProperties
    .filter(([, value]) => resolveSchema(value, api).format !== "binary")
    .map(([name, value]) => ({ name, value: multipartValue(name, value, api) }));
  const effectiveBody = body;
  const label = `${id} ${method} ${route}`;
  return `<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="${xml(label)}" enabled="true">
          ${argumentsXml(effectiveBody, formArguments)}
          ${fileParameter ? filesXml(fileParameter) : ""}
          ${prop("stringProp", "HTTPSampler.domain", "${host}", "          ")}
          ${prop("stringProp", "HTTPSampler.port", "${port}", "          ")}
          ${prop("stringProp", "HTTPSampler.protocol", "${protocol}", "          ")}
          ${prop("stringProp", "HTTPSampler.path", "${base_path}" + requestPath, "          ")}
          ${prop("stringProp", "HTTPSampler.method", method, "          ")}
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp><boolProp name="HTTPSampler.use_keepalive">true</boolProp>
          <boolProp name="HTTPSampler.DO_MULTIPART_POST">${Boolean(fileParameter)}</boolProp>
          <boolProp name="HTTPSampler.postBodyRaw">${body !== undefined}</boolProp>
        </HTTPSamplerProxy><hashTree>${assertionChildren(effectiveBody !== undefined, expectedPatternFor(method, route))}</hashTree>`;
}

const response = await fetch(openApiUrl);
if (!response.ok) throw new Error(`OpenAPI respondió ${response.status}: ${openApiUrl}`);
const api = await response.json();
const operations = [];
for (const [route, pathItem] of Object.entries(api.paths)) {
  for (const method of ["get", "post", "put", "patch", "delete"]) {
    if (pathItem[method]) operations.push({ route, method: method.toUpperCase(), operation: pathItem[method] });
  }
}
operations.sort((a, b) => `${a.route}|${a.method}`.localeCompare(`${b.route}|${b.method}`));
if (operations.length !== 158) throw new Error(`Se esperaban 158 endpoints y OpenAPI devolvió ${operations.length}`);

const variableElements = Object.entries(variables).map(([name, value]) => `<elementProp name="${name}" elementType="Argument">${prop("stringProp", "Argument.name", name)}${prop("stringProp", "Argument.value", value)}${prop("stringProp", "Argument.metadata", "=")}</elementProp>`).join("");
function credentialsFor(route, method = "GET") {
  if (route.startsWith("/clients/portal") || route.startsWith("/payments/portal")) {
    return { label: "OWNER", email: "${owner_email}", password: "${owner_password}" };
  }
  if (method === "DELETE" && (route.startsWith("/consultations") || route.startsWith("/prescriptions"))) {
    return { label: "ADMIN", email: "${email}", password: "${password}" };
  }
  if (route.startsWith("/consultations") || route.startsWith("/prescriptions") ||
      route.startsWith("/radiografia") || route.startsWith("/laboratorio") ||
      route.startsWith("/profile/schedule") || route.includes("/start") ||
      route.startsWith("/preventive-controls/consultations")) {
    return { label: "VET", email: "${vet_email}", password: "${vet_password}" };
  }
  return { label: "ADMIN", email: "${email}", password: "${password}" };
}

function loginFor(route) {
  const credentials = credentialsFor(route);
  return sampler(`AUTH-${credentials.label}`, "POST", "/auth/login", {
    requestBody: { content: { "application/json": { schema: { type: "object", properties: { email: { example: credentials.email }, password: { example: credentials.password } }, required: ["email", "password"] } } } },
  }, api);
}

const endpointFlows = operations.map((entry, index) => {
  const id = `EP-${String(index + 1).padStart(3, "0")}`;
  const endpoint = sampler(id, entry.method, entry.route, entry.operation, api);
  return `<GenericController guiclass="LogicControllerGui" testclass="GenericController" testname="Flujo ${id}" enabled="true" /><hashTree>${loginFor(entry.route)}${endpoint}</hashTree>`;
}).join("\n");

const document = `<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3"><hashTree>
<TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="VargasVet - 158 endpoints - 250 usuarios" enabled="true">
  <stringProp name="TestPlan.comments">Carga integral generada desde OpenAPI. 158 endpoints reales.</stringProp>
  <boolProp name="TestPlan.functional_mode">false</boolProp><boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
  <elementProp name="TestPlan.user_defined_variables" elementType="Arguments"><collectionProp name="Arguments.arguments" /></elementProp>
  <stringProp name="TestPlan.user_define_classpath" />
</TestPlan><hashTree>
  <Arguments guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables" enabled="true"><collectionProp name="Arguments.arguments">${variableElements}</collectionProp></Arguments><hashTree />
  <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="Accept JSON" enabled="true"><collectionProp name="HeaderManager.headers"><elementProp name="" elementType="Header"><stringProp name="Header.name">Accept</stringProp><stringProp name="Header.value">application/json</stringProp></elementProp></collectionProp></HeaderManager><hashTree />
  <CookieManager guiclass="CookiePanel" testclass="CookieManager" testname="Cookies por usuario" enabled="true"><collectionProp name="CookieManager.cookies" /><boolProp name="CookieManager.clearEachIteration">false</boolProp><boolProp name="CookieManager.controlledByThreadGroup">false</boolProp><stringProp name="CookieManager.policy">standard</stringProp></CookieManager><hashTree />
  <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="250 usuarios - todos los endpoints" enabled="true">
    <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
    <elementProp name="ThreadGroup.main_controller" elementType="LoopController"><boolProp name="LoopController.continue_forever">false</boolProp><stringProp name="LoopController.loops">\${__P(loops,-1)}</stringProp></elementProp>
    <stringProp name="ThreadGroup.num_threads">\${__P(users,250)}</stringProp><stringProp name="ThreadGroup.ramp_time">\${__P(ramp_seconds,300)}</stringProp>
    <boolProp name="ThreadGroup.scheduler">true</boolProp><stringProp name="ThreadGroup.duration">\${__P(duration_seconds,900)}</stringProp><stringProp name="ThreadGroup.delay">0</stringProp>
    <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
  </ThreadGroup><hashTree>
    <RandomController guiclass="RandomControlGui" testclass="RandomController" testname="Distribución entre 158 endpoints" enabled="true" /><hashTree>
      ${endpointFlows}
    </hashTree>
    <UniformRandomTimer guiclass="UniformRandomTimerGui" testclass="UniformRandomTimer" testname="Pausa" enabled="true"><stringProp name="ConstantTimer.delay">\${__P(think_time_ms,50)}</stringProp><stringProp name="RandomTimer.range">\${__P(think_time_range_ms,50)}</stringProp></UniformRandomTimer><hashTree />
  </hashTree>
</hashTree></hashTree></jmeterTestPlan>`;

fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, document, "utf8");
const loadController = `    <RandomController guiclass="RandomControlGui" testclass="RandomController" testname="Distribución entre 158 endpoints" enabled="true" /><hashTree>
      ${endpointFlows}
    </hashTree>`;
const smokeController = `    <GenericController guiclass="LogicControllerGui" testclass="GenericController" testname="Recorrido verificable de 158 endpoints" enabled="true" /><hashTree>
      ${endpointFlows}
    </hashTree>`;
const smokeDocument = document
  .replace("VargasVet - 158 endpoints - 250 usuarios", "VargasVet - humo verificable de 158 endpoints")
  .replace("250 usuarios - todos los endpoints", "1 usuario - recorrido de todos los endpoints")
  .replace("${__P(users,250)}", "${__P(users,1)}")
  .replace("${__P(ramp_seconds,300)}", "${__P(ramp_seconds,1)}")
  .replace("${__P(duration_seconds,900)}", "${__P(duration_seconds,600)}")
  .replace(loadController, smokeController);
fs.writeFileSync(smokeOutput, smokeDocument, "utf8");
console.log(`Planes JMeter generados: ${operations.length} endpoints -> ${output} | ${smokeOutput}`);

// Reemplaza los planes iniciales por una distribución realista: una sesión por
// usuario y grupos independientes para ADMIN, OWNER y VET. Así /auth/login no
// representa artificialmente la mitad del tráfico.
const roleOperations = operations.map((entry, index) => {
  const id = `EP-${String(index + 1).padStart(3, "0")}`;
  const credentials = credentialsFor(entry.route, entry.method);
  const operation = entry.route === "/auth/login"
    ? { requestBody: { content: { "application/json": { schema: {
        type: "object", properties: {
          email: { example: "${email}" }, password: { example: "${password}" },
        }, required: ["email", "password"],
      } } } } }
    : entry.operation;
  const trafficGroup = entry.route.startsWith("/auth/") ? "AUTH" : credentials.label;
  return { ...entry, id, credentials, trafficGroup, endpoint: sampler(id, entry.method, entry.route, operation, api) };
});

function roleLogin(credentials) {
  return sampler(`AUTH-${credentials.label}`, "POST", "/auth/login", {
    requestBody: { content: { "application/json": { schema: {
      type: "object", properties: {
        email: { example: credentials.email }, password: { example: credentials.password },
      }, required: ["email", "password"],
    } } } },
  }, api);
}

function roleThreadGroup(role, userProperty, defaultUsers, smoke) {
  const entries = roleOperations.filter((entry) => entry.trafficGroup === role);
  const credentials = entries[0].credentials;
  const flows = entries.map(({ id, endpoint }) =>
    `<GenericController guiclass="LogicControllerGui" testclass="GenericController" testname="Flujo ${id}" enabled="true" /><hashTree>${role === "AUTH" ? roleLogin(credentials) : ""}${endpoint}</hashTree>`
  ).join("\n");
  const controller = smoke
    ? `<GenericController guiclass="LogicControllerGui" testclass="GenericController" testname="Recorrido ${role}: ${entries.length} endpoints" enabled="true" /><hashTree>${flows}</hashTree>`
    : `<RandomController guiclass="RandomControlGui" testclass="RandomController" testname="Distribución ${role}: ${entries.length} endpoints" enabled="true" /><hashTree>${flows}</hashTree>`;
  const users = smoke ? "1" : `\${__P(${userProperty},${defaultUsers})}`;
  const loops = smoke ? "1" : "\${__P(loops,-1)}";
  const ramp = smoke ? "1" : "\${__P(ramp_seconds,300)}";
  const duration = smoke ? "600" : "\${__P(duration_seconds,900)}";
  return `<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="${role} - ${entries.length} endpoints" enabled="true">
    <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
    <elementProp name="ThreadGroup.main_controller" elementType="LoopController"><boolProp name="LoopController.continue_forever">false</boolProp><stringProp name="LoopController.loops">${loops}</stringProp></elementProp>
    <stringProp name="ThreadGroup.num_threads">${users}</stringProp><stringProp name="ThreadGroup.ramp_time">${ramp}</stringProp>
    <boolProp name="ThreadGroup.scheduler">true</boolProp><stringProp name="ThreadGroup.duration">${duration}</stringProp><stringProp name="ThreadGroup.delay">0</stringProp>
    <boolProp name="ThreadGroup.same_user_on_next_iteration">true</boolProp>
  </ThreadGroup><hashTree>
    <CookieManager guiclass="CookiePanel" testclass="CookieManager" testname="Sesión ${role}" enabled="true"><collectionProp name="CookieManager.cookies" /><boolProp name="CookieManager.clearEachIteration">false</boolProp><boolProp name="CookieManager.controlledByThreadGroup">false</boolProp><stringProp name="CookieManager.policy">standard</stringProp></CookieManager><hashTree />
    <OnceOnlyController guiclass="OnceOnlyControllerGui" testclass="OnceOnlyController" testname="Autenticación única ${role}" enabled="true" /><hashTree>${roleLogin(credentials)}</hashTree>
    ${controller}
    <UniformRandomTimer guiclass="UniformRandomTimerGui" testclass="UniformRandomTimer" testname="Pausa ${role}" enabled="true"><stringProp name="ConstantTimer.delay">\${__P(think_time_ms,50)}</stringProp><stringProp name="RandomTimer.range">\${__P(think_time_range_ms,50)}</stringProp></UniformRandomTimer><hashTree />
  </hashTree>`;
}

function roleDocument(smoke) {
  const groups = [
    roleThreadGroup("ADMIN", "admin_users", 145, smoke),
    roleThreadGroup("OWNER", "owner_users", 50, smoke),
    roleThreadGroup("VET", "vet_users", 50, smoke),
    roleThreadGroup("AUTH", "auth_users", 5, smoke),
  ].join("\n");
  const title = smoke ? "VargasVet - humo verificable de 158 endpoints" : "VargasVet - 158 endpoints - 250 usuarios";
  return `<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3"><hashTree>
<TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="${title}" enabled="true">
  <stringProp name="TestPlan.comments">158 endpoints reales separados por rol. Las mutaciones controladas pueden responder 4xx; ningún 5xx se considera aceptable.</stringProp>
  <boolProp name="TestPlan.functional_mode">false</boolProp><boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
  <elementProp name="TestPlan.user_defined_variables" elementType="Arguments"><collectionProp name="Arguments.arguments" /></elementProp><stringProp name="TestPlan.user_define_classpath" />
</TestPlan><hashTree>
  <Arguments guiclass="ArgumentsPanel" testclass="Arguments" testname="Variables" enabled="true"><collectionProp name="Arguments.arguments">${variableElements}</collectionProp></Arguments><hashTree />
  <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="Accept JSON" enabled="true"><collectionProp name="HeaderManager.headers"><elementProp name="" elementType="Header"><stringProp name="Header.name">Accept</stringProp><stringProp name="Header.value">application/json</stringProp></elementProp></collectionProp></HeaderManager><hashTree />
  ${groups}
</hashTree></hashTree></jmeterTestPlan>`;
}

fs.writeFileSync(output, roleDocument(false), "utf8");
fs.writeFileSync(smokeOutput, roleDocument(true), "utf8");
