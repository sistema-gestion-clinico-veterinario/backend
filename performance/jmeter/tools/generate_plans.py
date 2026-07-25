from pathlib import Path
from xml.etree.ElementTree import Element, SubElement, ElementTree, indent
ROOT = Path(__file__).resolve().parents[1]
PLANS = ROOT / "plans"
def prop(parent, tag, name, value):
    node = SubElement(parent, tag, {"name": name})
    node.text = str(value)
    return node
def component(tree, tag, gui, name, **attrs):
    values = {"guiclass": gui, "testclass": tag, "testname": name, "enabled": "true"}
    values.update(attrs)
    node = SubElement(tree, tag, values)
    children = SubElement(tree, "hashTree")
    return node, children
def arguments(parent, values):
    node, children = component(parent, "Arguments", "ArgumentsPanel", "Variables de la prueba")
    collection = SubElement(node, "collectionProp", {"name": "Arguments.arguments"})
    for index, (name, value) in enumerate(values.items()):
        item = SubElement(collection, "elementProp", {"name": name, "elementType": "Argument"})
        prop(item, "stringProp", "Argument.name", name)
        prop(item, "stringProp", "Argument.value", value)
        prop(item, "stringProp", "Argument.metadata", "=")
    return children
def raw_arguments(sampler, body):
    args = SubElement(sampler, "elementProp", {
        "name": "HTTPsampler.Arguments", "elementType": "Arguments",
        "guiclass": "HTTPArgumentsPanel", "testclass": "Arguments", "testname": "",
    })
    collection = SubElement(args, "collectionProp", {"name": "Arguments.arguments"})
    if body is not None:
        item = SubElement(collection, "elementProp", {"name": "", "elementType": "HTTPArgument"})
        prop(item, "boolProp", "HTTPArgument.always_encode", "false")
        prop(item, "stringProp", "Argument.value", body)
        prop(item, "stringProp", "Argument.metadata", "=")
def assertion(parent, expected="200", require_success=True):
    node, _ = component(parent, "ResponseAssertion", "AssertionGui", f"HTTP {expected}")
    tests = SubElement(node, "collectionProp", {"name": "Asserion.test_strings"})
    prop(tests, "stringProp", "1", expected)
    prop(node, "stringProp", "Assertion.custom_message", "Código HTTP inesperado")
    prop(node, "stringProp", "Assertion.test_field", "Assertion.response_code")
    prop(node, "boolProp", "Assertion.assume_success", "false")
    prop(node, "intProp", "Assertion.test_type", "8")
    if require_success:
        script, _ = component(parent, "JSR223Assertion", "TestBeanGUI", "Respuesta funcional exitosa")
        prop(script, "stringProp", "cacheKey", "true")
        prop(script, "stringProp", "filename", "")
        prop(script, "stringProp", "parameters", "")
        prop(script, "stringProp", "script", (
            "def text = prev.getResponseDataAsString()\n"
            "if (!(text.contains('\\\"success\\\":true') || text.contains('\\\"success\\\": true'))) {\n"
            "  AssertionResult.setFailure(true)\n"
            "  AssertionResult.setFailureMessage('La respuesta no contiene success=true')\n"
            "}"
        ))
        prop(script, "stringProp", "scriptLanguage", "groovy")
def http_sampler(parent, case_id, name, method, path, body=None, expected="200", extractor=None):
    sampler, children = component(parent, "HTTPSamplerProxy", "HttpTestSampleGui", f"{case_id} - {name}")
    raw_arguments(sampler, body)
    prop(sampler, "stringProp", "HTTPSampler.domain", "${host}")
    prop(sampler, "stringProp", "HTTPSampler.port", "${port}")
    prop(sampler, "stringProp", "HTTPSampler.protocol", "${protocol}")
    prop(sampler, "stringProp", "HTTPSampler.connect_timeout", "${connect_timeout_ms}")
    prop(sampler, "stringProp", "HTTPSampler.response_timeout", "${response_timeout_ms}")
    prop(sampler, "stringProp", "HTTPSampler.contentEncoding", "UTF-8")
    prop(sampler, "stringProp", "HTTPSampler.path", "${base_path}" + path)
    prop(sampler, "stringProp", "HTTPSampler.method", method)
    prop(sampler, "boolProp", "HTTPSampler.follow_redirects", "true")
    prop(sampler, "boolProp", "HTTPSampler.auto_redirects", "false")
    prop(sampler, "boolProp", "HTTPSampler.use_keepalive", "true")
    prop(sampler, "boolProp", "HTTPSampler.DO_MULTIPART_POST", "false")
    prop(sampler, "boolProp", "HTTPSampler.postBodyRaw", "true" if body is not None else "false")
    assertion(children, expected)
    if extractor:
        node, _ = component(children, "JSONPostProcessor", "JSONPostProcessorGui", "Extraer tokens")
        prop(node, "stringProp", "JSONPostProcessor.referenceNames", "access_token;refresh_token")
        prop(node, "stringProp", "JSONPostProcessor.jsonPathExprs", "$.data.token;$.data.refreshToken")
        prop(node, "stringProp", "JSONPostProcessor.match_numbers", "1;1")
        prop(node, "stringProp", "JSONPostProcessor.defaultValues", "NOT_FOUND;NOT_FOUND")
    return sampler, children
def base_plan(name):
    root = Element("jmeterTestPlan", {"version": "1.2", "properties": "5.0", "jmeter": "5.6.3"})
    root_tree = SubElement(root, "hashTree")
    plan, plan_tree = component(root_tree, "TestPlan", "TestPlanGui", name)
    prop(plan, "stringProp", "TestPlan.comments", "Suite VargasVet de pruebas de carga con Apache JMeter")
    prop(plan, "boolProp", "TestPlan.functional_mode", "false")
    prop(plan, "boolProp", "TestPlan.serialize_threadgroups", "false")
    user_vars = SubElement(plan, "elementProp", {
        "name": "TestPlan.user_defined_variables", "elementType": "Arguments",
        "guiclass": "ArgumentsPanel", "testclass": "Arguments", "testname": "Variables",
    })
    SubElement(user_vars, "collectionProp", {"name": "Arguments.arguments"})
    prop(plan, "stringProp", "TestPlan.user_define_classpath", "")
    variables = {
        "protocol": "${__P(protocol,http)}", "host": "${__P(host,127.0.0.1)}",
        "port": "${__P(port,8080)}", "base_path": "${__P(base_path,/api/v1)}",
        "company_id": "${__P(company_id,1)}", "pet_id": "${__P(pet_id,1)}",
        "employee_id": "${__P(employee_id,1)}", "service_id": "${__P(service_id,1)}",
        "appointment_date": "${__P(appointment_date,2026-12-15)}",
        "consultation_id": "${__P(consultation_id,0)}",
        "payment_appointment_id": "${__P(payment_appointment_id,1)}",
        "page_size": "${__P(page_size,10)}",
        "connect_timeout_ms": "${__P(connect_timeout_ms,5000)}",
        "response_timeout_ms": "${__P(response_timeout_ms,30000)}",
        "email": "${__groovy(System.getenv('LOAD_TEST_EMAIL') ?: '')}",
        "password": "${__groovy(System.getenv('LOAD_TEST_PASSWORD') ?: '')}",
    }
    arguments(plan_tree, variables)
    headers, _ = component(plan_tree, "HeaderManager", "HeaderPanel", "Cabeceras HTTP")
    header_collection = SubElement(headers, "collectionProp", {"name": "HeaderManager.headers"})
    for key, value in (("Content-Type", "application/json"), ("Accept", "application/json")):
        header = SubElement(header_collection, "elementProp", {"name": "", "elementType": "Header"})
        prop(header, "stringProp", "Header.name", key)
        prop(header, "stringProp", "Header.value", value)
    cookies, _ = component(plan_tree, "CookieManager", "CookiePanel", "Cookies por usuario")
    prop(cookies, "collectionProp", "CookieManager.cookies", "")
    prop(cookies, "boolProp", "CookieManager.clearEachIteration", "false")
    prop(cookies, "boolProp", "CookieManager.controlledByThreadGroup", "false")
    prop(cookies, "stringProp", "CookieManager.policy", "standard")
    return root, plan_tree
def thread_group(parent, name):
    group, tree = component(parent, "ThreadGroup", "ThreadGroupGui", name)
    prop(group, "stringProp", "ThreadGroup.on_sample_error", "continue")
    controller = SubElement(group, "elementProp", {
        "name": "ThreadGroup.main_controller", "elementType": "LoopController",
        "guiclass": "LoopControlPanel", "testclass": "LoopController", "testname": "Loop",
    })
    prop(controller, "boolProp", "LoopController.continue_forever", "false")
    prop(controller, "stringProp", "LoopController.loops", "${__P(loops,-1)}")
    prop(group, "stringProp", "ThreadGroup.num_threads", "${__P(users,1)}")
    prop(group, "stringProp", "ThreadGroup.ramp_time", "${__P(ramp_seconds,1)}")
    prop(group, "boolProp", "ThreadGroup.scheduler", "true")
    prop(group, "stringProp", "ThreadGroup.duration", "${__P(duration_seconds,30)}")
    prop(group, "stringProp", "ThreadGroup.delay", "0")
    prop(group, "boolProp", "ThreadGroup.same_user_on_next_iteration", "true")
    return tree
def build_read_plan(randomized=True):
    title = "VargasVet - carga PC-001 a PC-013" if randomized else "VargasVet - humo PC-001 a PC-013"
    root, plan_tree = base_plan(title)
    tree = thread_group(plan_tree, "Usuarios virtuales")
    once, once_tree = component(tree, "OnceOnlyController", "OnceOnlyControllerGui", "Autenticación por usuario")
    http_sampler(once_tree, "PC-001", "Inicio de sesión", "POST", "/auth/login",
                 '{"email":"${email}","password":"${password}"}', "200", extractor=True)
    http_sampler(once_tree, "PC-002", "Renovación del token", "POST", "/auth/refresh",
                 '{"refreshToken":"${refresh_token}"}', "200", extractor=True)
    if randomized:
        controller, request_tree = component(tree, "RandomController", "RandomControlGui", "Operación de lectura ponderada")
    else:
        controller, request_tree = component(tree, "GenericController", "LogicControllerGui", "Recorrido completo de lectura")
    requests = [
        ("PC-003", "Estadísticas del dashboard", "GET", "/dashboard/stats?companyId=${company_id}"),
        ("PC-004", "Listado de mascotas", "GET", "/pets?companyId=${company_id}&page=0&size=${page_size}"),
        ("PC-005", "Detalle de mascota", "GET", "/pets/${pet_id}"),
        ("PC-006", "Listado de citas", "GET", "/appointments?companyId=${company_id}&page=0&size=${page_size}"),
        ("PC-007", "Disponibilidad de citas", "GET", "/appointments/availability?empleadoId=${employee_id}&fecha=${appointment_date}&servicioId=${service_id}"),
        ("PC-008", "Servicios de una mascota", "GET", "/appointments/mascota/${pet_id}/servicios"),
        ("PC-009", "Listado de historias clínicas", "GET", "/medical-records?companyId=${company_id}&page=0&size=${page_size}"),
        ("PC-010", "Historia clínica de mascota", "GET", "/medical-records/pet/${pet_id}"),
        ("PC-012", "Controles preventivos", "GET", "/preventive-controls/pets/${pet_id}"),
        ("PC-013", "Historial de pagos", "GET", "/payments/history?companyId=${company_id}&page=0&size=${page_size}"),
    ]
    weights = {
        "PC-003": 10, "PC-004": 10, "PC-005": 10, "PC-006": 10, "PC-007": 10,
        "PC-008": 10, "PC-009": 5, "PC-010": 5, "PC-012": 10, "PC-013": 15,
    }
    for item in requests:
        repetitions = weights[item[0]] if randomized else 1
        for _ in range(repetitions):
            http_sampler(request_tree, item[0], item[1], item[2], item[3])
    consultation_repetitions = 5 if randomized else 1
    for _ in range(consultation_repetitions):
        condition, condition_tree = component(request_tree, "IfController", "IfControllerPanel", "PC-011 disponible")
        prop(condition, "stringProp", "IfController.condition", "${__groovy(vars.get('consultation_id') != '0')}")
        prop(condition, "boolProp", "IfController.evaluateAll", "false")
        prop(condition, "boolProp", "IfController.useExpression", "true")
        http_sampler(condition_tree, "PC-011", "Consulta clínica por ID", "GET", "/consultations/${consultation_id}")
    timer, _ = component(tree, "UniformRandomTimer", "UniformRandomTimerGui", "Pausa realista")
    prop(timer, "stringProp", "ConstantTimer.delay", "${__P(think_time_ms,500)}")
    prop(timer, "stringProp", "RandomTimer.range", "${__P(think_time_range_ms,1000)}")
    return root
def build_transaction_plan():
    root, plan_tree = base_plan("VargasVet - PC-014 a PC-017")
    tree = thread_group(plan_tree, "Transacciones controladas")
    once, once_tree = component(tree, "OnceOnlyController", "OnceOnlyControllerGui", "Autenticación por usuario")
    http_sampler(once_tree, "AUTH", "Inicio de sesión", "POST", "/auth/login",
                 '{"email":"${email}","password":"${password}"}', "200", extractor=True)
    csv, _ = component(tree, "CSVDataSet", "TestBeanGUI", "Datos transaccionales únicos")
    prop(csv, "stringProp", "delimiter", ",")
    prop(csv, "stringProp", "fileEncoding", "UTF-8")
    prop(csv, "stringProp", "filename", "${__P(transaction_csv,)}")
    prop(csv, "boolProp", "ignoreFirstLine", "true")
    prop(csv, "boolProp", "quotedData", "true")
    prop(csv, "boolProp", "recycle", "false")
    prop(csv, "stringProp", "shareMode", "shareMode.all")
    prop(csv, "boolProp", "stopThread", "true")
    prop(csv, "stringProp", "variableNames", "case_id,method,path,body,expected_code")
    sampler, children = http_sampler(tree, "${case_id}", "Operación transaccional", "${method}", "${path}", "${body}", "${expected_code}")
    return root


def write_plan(path, root):
    indent(root, space="  ")
    ElementTree(root).write(path, encoding="UTF-8", xml_declaration=True)


if __name__ == "__main__":
    PLANS.mkdir(parents=True, exist_ok=True)
    write_plan(PLANS / "vargasvet-smoke.jmx", build_read_plan(randomized=False))
    write_plan(PLANS / "vargasvet-read-load.jmx", build_read_plan(randomized=True))
    write_plan(PLANS / "vargasvet-transaction-load.jmx", build_transaction_plan())
    print("Planes JMeter generados correctamente.")
