import random
import string
import requests
import json
import uuid
import xml.etree.ElementTree as ET
from xml.dom import minidom


# --- TUS DATOS ---
marcas_modelos = [
    ("BMW", "Serie 1", "116d 85 kW", "Compacto"),
    ("BMW", "Serie 3", "320i 135 kW", "Berlina"),
    ("BMW", "X1", "sDrive18d 110 kW", "SUV"),
    ("Audi", "A3", "30 TDI 85 kW", "Berlina"),
    ("Audi", "A4", "35 TDI 120 kW", "Berlina"),
    ("Audi", "Q3", "35 TFSI 150 kW", "SUV"),
    ("Seat", "León", "1.5 TSI 130cv", "Compacto"),
    ("Seat", "Arona", "1.0 TSI 95cv", "SUV"),
    ("Seat", "Tarraco", "2.0 TDI 150cv", "SUV"),
    ("Volkswagen", "Golf", "2.0 TDI 115cv", "Compacto"),
    ("Volkswagen", "Passat", "2.0 TDI 140cv", "Berlina"),
    ("Volkswagen", "T-Roc", "1.5 TSI 150cv", "SUV"),
    ("Toyota", "Corolla", "140H Hybrid", "Hatchback"),
    ("Toyota", "RAV4", "2.5 Hybrid", "SUV"),
    ("Toyota", "Yaris", "1.5 Hybrid", "Compacto"),
    ("Ford", "Fiesta", "1.0 EcoBoost 100cv", "Compacto"),
    ("Ford", "Focus", "1.5 EcoBlue 120cv", "Berlina"),
    ("Ford", "Kuga", "2.0 Hybrid 140cv", "SUV"),
    ("Mercedes-Benz", "A-Class", "A200 120kW", "Compacto"),
    ("Mercedes-Benz", "C-Class", "C200 150kW", "Berlina"),
    ("Mercedes-Benz", "GLA", "200d 110kW", "SUV"),
    ("Honda", "Civic", "1.5 VTEC 182cv", "Compacto"),
    ("Honda", "CR-V", "2.0 i-MMD Hybrid", "SUV"),
    ("Nissan", "Leaf", "40 kWh", "Compacto Eléctrico"),
    ("Nissan", "Qashqai", "1.3 DIG-T 140cv", "SUV"),
    ("Hyundai", "i20", "1.0 T-GDI 100cv", "Compacto"),
    ("Hyundai", "Tucson", "1.6 Hybrid 150cv", "SUV"),
    ("Kia", "Ceed", "1.4 T-GDI 140cv", "Compacto"),
    ("Kia", "Sportage", "1.6 CRDi 136cv", "SUV"),
    ("Peugeot", "208", "1.2 PureTech 100cv", "Compacto"),
    ("Peugeot", "3008", "1.5 BlueHDi 130cv", "SUV"),
    ("Renault", "Clio", "1.0 TCe 100cv", "Compacto"),
    ("Renault", "Captur", "1.3 TCe 140cv", "SUV"),
    ("Mazda", "3", "2.0 Skyactiv-G 122cv", "Compacto"),
    ("Mazda", "CX-5", "2.2 Skyactiv-D 150cv", "SUV"),
    ("Volvo", "XC40", "T4 140kW", "SUV"),
    ("Volvo", "S60", "B4 Mild Hybrid 197cv", "Berlina"),
    ("Jaguar", "XE", "2.0d 163cv", "Berlina"),
    ("Jaguar", "F-Pace", "2.0d 204cv", "SUV"),
    ("Lexus", "NX", "350h Hybrid", "SUV"),
    ("Lexus", "IS", "300h Hybrid", "Berlina"),
    ("Mini", "Cooper", "1.5 136cv", "Compacto"),
    ("Mini", "Countryman", "1.5 136cv", "SUV"),
    ("Porsche", "Macan", "2.0 245cv", "SUV"),
    ("Porsche", "911", "3.0 Carrera 385cv", "Deportivo"),
    ("Tesla", "Model 3", "AWD Long Range", "Sedán Eléctrico"),
    ("Tesla", "Model Y", "AWD Long Range", "SUV Eléctrico"),
    ("Citroën", "C3", "1.2 PureTech 110cv", "Compacto"),
    ("Citroën", "C5 Aircross", "1.5 BlueHDi 130cv", "SUV"),
    ("Opel", "Corsa", "1.2 100cv", "Compacto"),
    ("Opel", "Grandland", "1.5 Diesel 130cv", "SUV"),
    ("Fiat", "500", "1.0 Hybrid 70cv", "Microcompacto"),
    ("Fiat", "Panda", "1.0 Hybrid 70cv", "Compacto")
]

def generar_uuid():
    nuevo_id = uuid.uuid4()
    return str(nuevo_id)

def generar_matricula():
    numeros = "".join(random.choices(string.digits, k=4))
    letras = "".join(random.choices("BCDFGHJKLMNPQRSTVWXYZ", k=3))
    return f"{numeros}{letras}"

def generar_vin():
    return "".join(random.choices(string.ascii_uppercase + string.digits, k=17))

def generar_body_type():
    opciones = [
        {"key": "1", "value": "Compact or small"},
        {"key": "2", "value": "Saloon or sedan"},
        {"key": "3", "value": "Family car"},
        {"key": "4", "value": "SUV, all-terrain or 4x4"},
        {"key": "5", "value": "Convertible"},
        {"key": "6", "value": "Sport or coupe"},
        {"key": "9", "value": "Minivan"},
        {"key": "10", "value": "Pickup"},
        {"key": "23", "value": "Van"}
    ]
    return random.choice(opciones)

def generar_change_type():
    opciones = [
        {"key": "A", "value": "Automatic"},
        {"key": "M", "value": "Manual"}
    ]
    return random.choice(opciones)

def generar_fuel_type():
    opciones = [
        {"key": "G", "value": "Gasoline"},
        {"key": "D", "value": "Diesel"},
        {"key": "H", "value": "Hybrid"},
        {"key": "E", "value": "Electric"},
        {"key": "L", "value": "GLP"},
        {"key": "N", "value": "GNC"},
        {"key": "M", "value": "Etanol + Gasolina/Bio"},
        {"key": "HG", "value": "Hybrid Gasoline"},
        {"key": "HD", "value": "Hybrid Diesel"}
    ]
    return random.choice(opciones)

def generar_drivetraintype_type():
    opciones = [
        {"key": "FWD", "value": "Front wheel drive"},
        {"key": "RWD", "value": "Rear wheel drive"},
        {"key": "AWD", "value": "All wheel drive"},
        {"key": "4X4", "value": "4x4"}
    ]
    return random.choice(opciones)

def generar_environmentalbadge_type():
    opciones = [
        {"key": "0", "value": "0"},
        {"key": "ECO", "value": "ECO"},
        {"key": "C", "value": "C"},
        {"key": "B", "value": "B"},
        {"key": "None", "value": "None"}
    ]
    return random.choice(opciones)

def generar_color_type():
    opciones = [
        {"key": "1", "value": "Yellow"},
        {"key": "2", "value": "Blue"},
        {"key": "3", "value": "Light blue"},
        {"key": "4", "value": "Beige"},
        {"key": "5", "value": "White"},
        {"key": "6", "value": "Bronze"},
        {"key": "7", "value": "Gold color"},
        {"key": "8", "value": "Grey"},
        {"key": "9", "value": "Light grey"},
        {"key": "10", "value": "Brown"},
        {"key": "11", "value": "Orange"},
        {"key": "12", "value": "Black"},
        {"key": "13", "value": "Silver"},
        {"key": "14", "value": "Red"},
        {"key": "15", "value": "Dark red"},
        {"key": "16", "value": "Green"},
        {"key": "17", "value": "Light green"},
        {"key": "18", "value": "Violet"},
        {"key": "19", "value": "Grenade"}
    ]
    return random.choice(opciones)

def generar_equipment():
    opciones = [
        {"key": "1", "value": "ABS"},
        {"key": "2", "value": "Driver-side_airbag"},
        {"key": "3", "value": "Passenger-side_airbag"},
        {"key": "4", "value": "Sunroof"},
        {"key": "5", "value": "Radio"},
        {"key": "6", "value": "Power_windows"},
        {"key": "7", "value": "Alloy_wheels"},
        {"key": "8", "value": "Central_door_lock"},
        {"key": "9", "value": "Alarm_system"},
        {"key": "11", "value": "Navigation_system"},
        {"key": "15", "value": "Seat_heating"},
        {"key": "18", "value": "Cruise_control"},
        {"key": "21", "value": "Electronic_stability_control"},
        {"key": "24", "value": "Air_conditioning"},
        {"key": "28", "value": "Automatic_climate_control"},
        {"key": "37", "value": "Panorama_roof"},
        {"key": "44", "value": "Start-stop_system"},
        {"key": "45", "value": "Multifunction_steering_wheel"},
        {"key": "52", "value": "Bluetooth"},
        {"key": "55", "value": "Isofix"},
        {"key": "60", "value": "Parking_assist_system_camera"},
        {"key": "70", "value": "LED_Headlights"},
        {"key": "80", "value": "Tire_pressure_monitoring_system"},
        {"key": "83", "value": "Keyless_central_door_lock"},
        {"key": "89", "value": "Touch_screen"},
        {"key": "91", "value": "USB"}
    ]
    return random.choice(opciones)

def generar_equipment_category():
    opciones = [
        {"key": "Serial", "value": "Serial equipment"},
        {"key": "Extra", "value": "Extra equipment"},
        {"key": "Other", "value": "Other equipment"}
    ]
    return random.choice(opciones)

def generar_equipment_type():
    opciones = [
        {"key": "Outside", "value": "Outside equipment"},
        {"key": "Inside", "value": "Inside equipment"},
        {"key": "Security", "value": "Security equipment"},
        {"key": "Comfort", "value": "Comfort equipment"},
        {"key": "Other", "value": "Other equipment"}
    ]
    return random.choice(opciones)

def generar_equipments():
    cantidad = random.randint(2, 5)
    lista_equipments = []
    for _ in range(cantidad):
        item = {
            "id": random.randint(0, 50),
            "equipment": generar_equipment(),
            "category": generar_equipment_category(),
            "type": generar_equipment_type(),
            "description": "Esto es una descripción",
            "code": "CODE",
            "price": {
                "amount": 0.00,
                "currency": "EUR"
            }
        }
        lista_equipments.append(item)

    return lista_equipments

def generar_state_type():
    opciones = [
        {"key": "New", "value": "New"},
        {"key": "Used", "value": "Used"},
        {"key": "Opportunity", "value": "Opportunity"},
        {"key": "KM0", "value": "KM0 / Pre-registered"}
    ]
    return random.choice(opciones)

def generar_resources(cantidad=3):
    resources_list = []
    url_template = "https://fotos.estaticosmf.com/fotos_anuncios/00/01/67/02/87/0/x{:02d}.jpg"

    numeros_seleccionados = random.sample(range(1, 55), cantidad)

    for num in numeros_seleccionados:
        recurso = {
            "id": random.randint(1000, 99999),
            "type": {
                "key": "UrlImage",
                "value": "Image by URL"
            },
            "resource": url_template.format(num),
            "compressedResource": None
        }
        resources_list.append(recurso)

    return resources_list

def generar_offer(indice):
    marca, modelo, version, carroceria = random.choice(marcas_modelos)
    precio = random.randint(15000, 45000)
    urban = round(random.uniform(5.5, 8.0), 1)
    road = round(random.uniform(4.0, 5.4), 1)
    avg = round((urban + road) / 2, 1)

    # Retorna la estructura exacta solicitada
    return {
            "id": generar_uuid(),
            "sellerType": "usedCar_ProfessionalSeller",
            "privateOwnerRegisteredUserId": None,
            "hash": random.randint(100000000, 999999999),
            "vehicleInstance": {
                "id": random.randint(10000, 99999),
                "vehicleModel": {
                    "id": random.randint(100, 999),
                    "brand": marca,
                    "model": modelo,
                    "version": version,
                    "bodyType": generar_body_type(),
                    "numDoors": random.choice([2, 3, 4, 5]),
                    "cv": random.randint(90, 400),
                    "numCylinders": random.choice([3, 4, 6]),
                    "displacement": random.randint(999, 2999),
                    "urbanConsumption": urban,
                    "roadConsumption": road,
                    "avgConsumption": avg,
                    "numGears": 7,
                    "kgWeight": random.randint(800, 2000),
                    "changeType": generar_change_type(),
                    "fuelType": generar_fuel_type(),
                    "numSeats": 5,
                    "drivetrainType": generar_drivetraintype_type(),
                    "euroTaxCode": f"ET{random.randint(100000, 999999)}",
                    "environmentalBadge": generar_environmentalbadge_type(),
                    "cmWidth": 180,
                    "cmLength": 450,
                    "cmHeight": 145,
                    "litresTrunk": 450,
                    "litresTank": 55,
                    "maxSpeed": 210,
                    "maxEmissions": 120,
                    "acceleration": 7.5
                },
                "plate": generar_matricula(),
                "color": generar_color_type(),
                "mileage": random.randint(10000, 150000),
                "registrationYear": random.randint(2018, 2024),
                "registrationMonth": random.randint(1, 12),
                "isMetallicPaint": True,
                "chassisNumber": generar_vin(),
                "equipments": generar_equipments(),
                "state": generar_state_type()
            },
            "ownerReference": f"DEALER_{indice:03d}",
            "dealerReference": f"REF-{marca.upper()}-{random.randint(100,999)}",
            "channelReference": "API_IMPORT",
            "price": {"amount": float(precio), "currency": "EUR"},
            "financedPrice": {"amount": float(precio * 0.95), "currency": "EUR"},
            "financedInstallmentAprox": {"amount": 350.0, "currency": "EUR"},
            "financedText": "Cuota calculada según condiciones generales.",
            "priceNew": {"amount": float(precio * 1.4), "currency": "EUR"},
            "professionalPrice": {"amount": float(precio * 0.9), "currency": "EUR"},
            "taxDeductible": True,
            "obs": "Vehículo disponible para entrega inmediata.",
            "internalNotes": "Revisión pre-entrega realizada.",
            "resources": generar_resources(),
            "guarantee": True,
            "guaranteeMonths": 12,
            "guaranteeText": "Garantía total de 12 meses.",
            "certified": True,
            "installation": "Concesionario Central",
            "mail": "contacto@concesionario.es",
            "pickUpAddress": {
                "type": "B_PICKUP",
                "address": {
                    "name": "Punto de Entrega Principal",
                    "gpsLocation": {"name": "Madrid", "longitude": -3.68, "latitude": 40.41},
                    "streetNumber": "10",
                    "streetAddress": "Calle Inventada",
                    "postalCode": "28001",
                    "city": "Madrid",
                    "region": "Madrid",
                    "country": "España",
                    "countryCode": "ES",
                    "type": "business"
                }
            },
            "jsonCarOfferId": generar_uuid()
        }

def generar_xml_motorflash(indice):
    # --- OBTENEMOS DATOS ALEATORIOS USANDO TUS FUNCIONES ---
    marca, modelo, version, carroceria_txt = random.choice(marcas_modelos)

    # Generamos valores técnicos usando tus lógicas de random
    urban = round(random.uniform(5.5, 8.0), 1)
    road = round(random.uniform(4.0, 5.4), 1)
    avg = round((urban + road) / 2, 1)

    # Obtenemos objetos complejos de tus funciones
    fuel = generar_fuel_type()
    change = generar_change_type()
    color = generar_color_type()
    badge = generar_environmentalbadge_type()
    state = generar_state_type()
    resources = generar_resources(random.randint(5, 15)) # Usamos tu sample de 1-54
    equipments = generar_equipments() # Tu lista de 2-5 equipos complejos

    # --- CONSTRUCCIÓN DEL XML ---
    motorflash = ET.Element("motorflash")
    anuncio = ET.SubElement(motorflash, "anuncio")

    # Mapeo de campos simples
    ET.SubElement(anuncio, "motorflashid").text = generar_uuid()
    ET.SubElement(anuncio, "dealerid").text = "211"
    ET.SubElement(anuncio, "instalacion").text = "Canalcar Automoviles"
    ET.SubElement(anuncio, "mail").text = "ventas@canalcar.es"
    ET.SubElement(anuncio, "matricula").text = generar_matricula()
    ET.SubElement(anuncio, "chasis").text = generar_vin()

    ET.SubElement(anuncio, "marca").text = marca
    ET.SubElement(anuncio, "modelo").text = modelo
    ET.SubElement(anuncio, "version").text = version
    ET.SubElement(anuncio, "carroceria").text = carroceria_txt
    ET.SubElement(anuncio, "estado").text = state["value"].lower() # "Used" -> "used"
    ET.SubElement(anuncio, "disponible").text = "DISPONIBLE"

    precio = random.randint(15000, 45000)
    ET.SubElement(anuncio, "precio").text = str(precio)
    ET.SubElement(anuncio, "precio_nuevo").text = str(int(precio * 1.4))
    ET.SubElement(anuncio, "precio_profesional").text = str(int(precio * 0.9))
    ET.SubElement(anuncio, "precio_ofertafinanciacion").text = str(int(precio * 0.95))

    # Datos técnicos con tus randoms
    ET.SubElement(anuncio, "puertas").text = str(random.choice([3, 5]))
    ET.SubElement(anuncio, "cambio").text = change["value"]
    ET.SubElement(anuncio, "ancho").text = "1800"
    ET.SubElement(anuncio, "largo").text = "4500"
    ET.SubElement(anuncio, "alto").text = "1450"
    ET.SubElement(anuncio, "maletero").text = "450"
    ET.SubElement(anuncio, "deposito").text = "55"
    ET.SubElement(anuncio, "aceleracion").text = "7.5"
    ET.SubElement(anuncio, "velocidadmax").text = "210"
    ET.SubElement(anuncio, "peso").text = str(random.randint(1200, 1800))
    ET.SubElement(anuncio, "marchas").text = "7"
    ET.SubElement(anuncio, "kilometros").text = str(random.randint(10000, 150000))
    ET.SubElement(anuncio, "combustible").text = fuel["value"]
    ET.SubElement(anuncio, "potencia").text = str(random.randint(90, 300))
    ET.SubElement(anuncio, "garantia").text = "12"
    ET.SubElement(anuncio, "iva_deducible").text = "true"
    ET.SubElement(anuncio, "plazas").text = "5"
    ET.SubElement(anuncio, "traccion").text = generar_drivetraintype_type()["value"]
    ET.SubElement(anuncio, "fechamatriculacion").text = f"01 / 01 / {random.randint(2018, 2024)}"

    # --- FOTOS (Usando tu función generar_resources) ---
    fotos_node = ET.SubElement(anuncio, "fotos")
    for res in resources:
        ET.SubElement(fotos_node, "foto").text = res["resource"]

    ET.SubElement(anuncio, "color").text = color["value"]
    ET.SubElement(anuncio, "distintivo").text = badge["key"]

    # --- CONSUMO (Tus lógicas de uniform) ---
    cons = ET.SubElement(anuncio, "consumo")
    ET.SubElement(cons, "carretera").text = str(road)
    ET.SubElement(cons, "urbano").text = str(urban)
    ET.SubElement(cons, "mixto").text = str(avg)

    # --- EXTRAS (Usando tu función generar_equipments) ---
    extras_node = ET.SubElement(anuncio, "extras")
    for eq in equipments:
        # Motorflash usa el formato <extra cod="X" precio="Y">Nombre</extra>
        ext = ET.SubElement(extras_node, "extra",
                            cod=str(eq["id"]),
                            precio=str(eq["price"]["amount"]))
        ext.text = eq["equipment"]["value"] # Accedemos al value del objeto equipment

    # --- OBSERVACIONES Y NOTA ---
    ET.SubElement(anuncio, "observaciones").text = "Generado aleatoriamente para pruebas API."
    ET.SubElement(anuncio, "notainterna").text = generar_uuid()

    # Serialización
    xml_string = ET.tostring(motorflash, encoding='utf-8')
    reparsed = minidom.parseString(xml_string)
    return reparsed.toprettyxml(indent="  ")


# --- ENTRADA DE DATOS ---
print("--- CONFIGURACIÓN DE ENVÍO API ---")
try:
    total_vehiculos = int(input("¿Cuántas ofertas quieres enviar?: "))
    print("\nSelecciona el formato de envío:")
    print("1. batch")
    print("2. url")
    print("3. stream")
    tipo_llamada = input("Elige una opción: ")
except ValueError:
    print("❌ Error: Número no válido.")
    exit()

# --- TOKEN ---
url = "http://localhost:8888/api/v1/security/login"
payload = json.dumps({
    "client": "cf68a603-9d8f-44bd-9ac3-398d61aa3182",
    "secret": ".Liquidcars1",
    "securityProfile": "M2M_B2C_Channel"
})
headers = {
    'Content-Type': 'application/json'
}
response = requests.request("POST", url, headers=headers, data=payload)
response_data = response.json()
token_input = response_data.get("access_token")

# --- DEFINICIÓN DE URL Y HEADERS ---
url_api = f"http://localhost:8890/{tipo_llamada}"

headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {token_input}"
}

print(f"\n🚀 Iniciando envío a {url_api}...\n")

exitos = 0
errores = 0

if tipo_llamada == "batch":
    for i in range(total_vehiculos):
        # Pasamos el índice i para la referencia
        offer_obj = generar_offer(i)
        payload = [offer_obj]

        try:
            response = requests.post(url_api, headers=headers, json=payload, timeout=10)

            if response.ok:
                exitos += 1
                print(f"[{i+1}/{total_vehiculos}] ✅ OK: {offer_obj['id']}")

            else:
                errores += 1
                print(f"[{i+1}/{total_vehiculos}] ❌ Error {response.status_code}: {response.text}")
                if response.status_code == 401:
                    print("🛑 Token caducado. Abortando...")
                    break
        except Exception as e:
            errores += 1
            print(f"[{i+1}/{total_vehiculos}] ⚠️ Fallo de conexión: {e}")

print(f"\n--- RESUMEN FINAL ---")
print(f"✅ Éxitos: {exitos}")
print(f"❌ Errores: {errores}")
input("\nPresiona Enter para salir...")