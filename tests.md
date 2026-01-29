# 🧪 Estado de la Suite de Tests - LiquidCars Ingestion API

Este documento resume la cobertura de pruebas implementada con un total de 23 tests.

---

## 🏗️ Resumen por Capas

| Capa                    | Módulo                         | Estado | Cantidad | Clases                                                                                                                              |
|:------------------------|:-------------------------------|:------:|:--------:|:------------------------------------------------------------------------------------------------------------------------------------|
| **Application**         | `ingestion-application`        |   ✅    |    10    | OfferItemWriterTest, OfferStreamItemReaderTest, OfferJSONProcessorTest,OfferXmlProcessorTest, OfferIngestionProcessServiceImplTest. |
| **Boot**                | `ingestion-boot`               |   ✅    |    1     | IngestionApiApplicationTests.                                                                                                       |
| **Domain**              | `ingestion-domain`             |   ⚙️   |    0     | No tests.                                                                                                                           |
| **Factory**             | `ingestion-factory-data-test`  |   ⚙️   |    0     | No tests.                                                                                                                           |
| **Consumidor de Kafka** | `ingestion-infra-input-kafka`  |   ✅    |    5     | OfferInfraKafkaConsumerTest, OfferInfraKafkaConsumerServiceImplTest.                                                                |
| **API**                 | `ingestion-infra-input-rest`   |   ✅    |    3     | IngestionControllerTest.                                                                                                            |
| **MongoDB**             | `ingestion-infra-mongodb`      | ️️   ✅ |    1     | OfferInfraNoSQLServiceImplTest.                                                                                                     |
| **Productor de Kafka**  | `ingestion-infra-output-kafka` |   ✅    |    2     | OfferKafkaPublisherTest, OfferInfraKafkaProducerServiceImplTest.                                                                    |
| **PostgreSQL**          | `ingestion-infra-postgresql`   |   ✅    |    1     | OfferInfraSQLServiceImplTest.                                                                                                       |



---

## 📂 Detalle de Componentes Testeados

### 1. Capa de Aplicación (`ingestion-application`)

* **`OfferItemWriterTest`**: Verificación del cierre del pipeline de Spring Batch.
    * **Escenarios**:
        * **Procesamiento de Chunks**: Valida que cada oferta del lote sea enviada individualmente al productor de Kafka.
        * **Integridad del Lote**: Garantiza que no existan fugas de datos entre la fase de procesamiento y la de salida.
    * **Técnica**: Aislamiento con Mocks y verificación de interacción por conteo (`verify times`).
  
* **`OfferStreamItemReaderTest`**: Validación de la lectura reactiva y asíncrona.
    * **Escenarios**:
        * **Flujo Completo**: Comprueba que el lector entrega todas las ofertas procesadas por el parser y emite una señal de fin (`null`) al terminar el stream.
        * **Gestión de Errores**: Verifica que las excepciones ocurridas en el hilo virtual del parser se propagan correctamente al hilo principal del Batch.
    * **Técnica**: Uso de `doAnswer` para simular callbacks asíncronos y aserciones temporales para validar la comunicación entre hilos.

* **`OfferJSONProcessorTest`**: Validación del motor de parseo de archivos JSON.
    * **Escenarios**:
        * **Parseo Masivo**: Asegura que el procesador recorre correctamente un stream JSON y mapea la totalidad de los anuncios (10/10) a objetos de dominio.
        * **Precisión de Tipos**: Verifica el correcto mapeo de fechas (`OffsetDateTime`), precisiones numéricas (`BigDecimal`) y coherencia de Enums.
    * **Técnica**: Uso de archivos físicos de prueba (`testFiles`) y configuración de Jackson optimizada para el estándar ISO-8601.

* **`OfferXmlProcessorTest`**: Verificación de la ingesta y deserialización de datos en formato XML.
    * **Escenarios**:
        * **Procesamiento por Eventos**: Comprueba que el motor XML detecta y procesa correctamente los 10 registros del archivo de prueba, notificando al consumidor por cada oferta encontrada.
        * **Fidelidad de Datos**: Asegura que el mapeo desde etiquetas XML a objetos de dominio mantiene la precisión en campos complejos como marcas, precios y fechas con zona horaria.
    * **Técnica**: Uso de `ArgumentCaptor` para interceptar y validar el contenido enviado al consumidor y archivos reales para garantizar la compatibilidad con el esquema XML definido.

* **`OfferIngestionProcessServiceImplTest`**: Test del orquestador central de ingesta.
    * **Escenarios**:
        * **Selección Dinámica de Parser**: Garantiza que el sistema identifica y utiliza el parser correcto (JSON/XML) basándose en el formato de entrada, ignorando los no compatibles.
        * **Disparo de Jobs Asíncronos**: Verifica que las ingestas masivas (Stream/URL) inician correctamente un proceso de Spring Batch mediante el `JobLauncher`.
        * **Validación de Formatos**: Comprueba que el sistema lanza una excepción controlada (`IllegalArgumentException`) si se intenta procesar un formato no soportado.
    * **Técnica**: Uso de `ArgumentCaptor` para inspeccionar la lógica de negocio y `verify` con `timeout` para validar ejecuciones en hilos separados.

### 2. Capa de Arranque (`ingestion-boot`)

* **`IngestionApiApplicationTests`**: Prueba de carga del contexto completo de Spring Boot.
    * **Escenarios**:
        * **Validación de Inyección**: Garantiza que todos los Beans de la arquitectura hexagonal (Domain, Application e Infra) están correctamente definidos y se pueden inyectar sin conflictos.
        * **Integridad de Configuración**: Verifica de forma implícita que los archivos de propiedades, esquemas de Liquibase y configuraciones de seguridad son válidos.
    * **Técnica**: Uso de `@SpringBootTest` para levantar el ecosistema completo de la aplicación, actuando como la última línea de defensa antes del despliegue.

### 3. Capa de Dominio (`ingestion-domain`)

* No tests.

### 4. Capa de Factoría de Tests  (`ingestion-factory-data-test`)

* No tests.

### 5. Capa de Consumidor de Kafka (`ingestion-infra-input-kafka`)

* **`OfferInfraKafkaConsumerTest`**: Validación del adaptador de entrada de mensajería (Kafka).
    * **Escenarios**:
        * **Consumo y Delegación**: Asegura que el mensaje recibido (`OfferMsg`) se transforma correctamente al modelo de dominio y se entrega al servicio de persistencia.
        * **Tolerancia a Fallos**: Verifica que los errores de mapeo o procesamiento se capturan adecuadamente (try-catch), evitando que el consumidor se detenga o entre en bucles de reintento infinitos por excepciones no controladas.
    * **Técnica**: Mocks de servicios y mappers con verificaciones de comportamiento (`never()`, `times(1)`) para asegurar el flujo lógico.

* **`OfferInfraKafkaConsumerServiceImplTest`**: Validación de la persistencia políglota y resiliencia.
    * **Escenarios**:
        * **Persistencia Dual**: Garantiza que cada oferta se guarde de forma sincronizada tanto en el sistema **SQL (PostgreSQL)** como en el **NoSQL (MongoDB)**.
        * **Aislamiento de Fallos**: Verifica la independencia de los sistemas de guardado; el fallo crítico de una base de datos (ej. "Mongo Down") no interrumpe ni impide el guardado en la otra.
    * **Técnica**: Simulación de errores controlados (`doThrow`) para validar que el flujo de ejecución no se detiene ante excepciones de infraestructura.

### 6. Capa de API  (`ingestion-infra-input-rest`)

* **`IngestionControllerTest`**: Validación del adaptador de entrada REST y cumplimiento de contrato.
    * **Escenarios**:
        * **Gestión de Protocolos**: Asegura que los endpoints responden con los códigos de estado adecuados según el tipo de operación (`200 OK` para síncronos, `202 Accepted` para procesos asíncronos/streaming).
        * **Validación de Payloads**: Verifica que el controlador acepta y procesa correctamente diferentes tipos de contenido (JSON, Parámetros de URL y Streams binarios).
        * **Integridad del Puerto**: Confirma que las peticiones externas se delegan correctamente al servicio de aplicación tras el mapeo.
    * **Técnica**: Uso de `MockMvc` para pruebas de caja negra de la capa web y `@MockitoBean` para el aislamiento de dependencias en Spring Boot 3.4.

### 6. Capa de MongoDB  (`ingestion-infra-mongodb`)

* **`OfferInfraNoSQLServiceImplTest`**: Validación del adaptador de persistencia NoSQL (MongoDB).
    * **Escenarios**:
        * **Mapeo a Documento**: Garantiza que el DTO de dominio se transforma correctamente en una entidad de MongoDB (`OfferNoSQLEntity`) antes de persistir.
        * **Delegación al Repositorio**: Verifica que se invoca el método de guardado del repositorio de Spring Data tras el mapeo.
    * **Técnica**: Aislamiento total mediante Mocks para validar la lógica del servicio sin requerir una conexión activa a base de datos.

### 7. Capa de Productor de Kafka  (`ingestion-infra-output-kafka`)

* **`OfferKafkaPublisherTest`**: Validación del emisor de eventos de infraestructura.
    * **Escenarios**:
        * **Enrutamiento de Mensajes**: Garantiza que las ofertas procesadas se envíen al topic correcto (`...create-action.0`).
        * **Estrategia de Particionado**: Verifica que se utiliza el ID de la oferta como clave (`key`) del mensaje para asegurar el orden de procesamiento en Kafka.
    * **Técnica**: Mocking del `KafkaTemplate` de Spring para interceptar y validar los parámetros de envío.

* **`OfferInfraKafkaProducerServiceImplTest`**: Orquestación del flujo de salida de eventos.
    * **Escenarios**:
        * **Integración de Salida**: Valida que la oferta cruza la frontera del hexágono siendo transformada correctamente del modelo de dominio al esquema de mensaje de Kafka.
        * **Delegación de Envío**: Asegura que una vez mapeado el mensaje, se invoca al componente de transporte (`Publisher`) para su publicación definitiva.
    * **Técnica**: Verificación de comportamiento mediante Mocks para garantizar el desacoplamiento entre el mapeo y el transporte.

### 7. Capa de PostgreSQL  (`ingestion-infra-postgresql`)

* **`OfferInfraSQLServiceImplTest`**: Validación del adaptador de persistencia relacional (JPA).
    * **Escenarios**:
        * **Mapeo y Guardado**: Asegura que el flujo de persistencia SQL transforma correctamente el DTO de dominio y delega el almacenamiento al repositorio de Spring Data JPA.
    * **Técnica**: Aislamiento de infraestructura mediante Mocks para verificar la interacción entre componentes sin requerir una base de datos real.