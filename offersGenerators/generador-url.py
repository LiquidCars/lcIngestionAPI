import random
import string
import requests
import json
import uuid
import xml.etree.ElementTree as ET
from xml.dom import minidom
from datetime import datetime
import os


# --- DATOS Y GENERADORES ---
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

    cantidad_real = min(cantidad, 54)
    numeros_seleccionados = random.sample(range(1, 55), cantidad_real)

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

def generar_email():
    dominios = ["gmail.com", "hotmail.com", "yahoo.es", "outlook.com", "concesionario.es", "bmw.es", "audi.es"]
    nombres = ["ventas", "info", "contacto", "comercial", "atencion", "soporte", "admin"]

    nombre = random.choice(nombres)
    numero = random.randint(1, 999)
    dominio = random.choice(dominios)

    return f"{nombre}{numero}@{dominio}"

def generar_address():
    direcciones = [
        {
            "name": "Concesionario Madrid Centro",
            "gpsLocation": {"name": "Madrid", "longitude": -3.6883, "latitude": 40.4168},
            "streetNumber": "45",
            "streetAddress": "Calle de Serrano",
            "postalCode": "28001",
            "city": "Madrid",
            "region": "Madrid",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "Automoción Barcelona",
            "gpsLocation": {"name": "Barcelona", "longitude": 2.1734, "latitude": 41.3851},
            "streetNumber": "123",
            "streetAddress": "Passeig de Gràcia",
            "postalCode": "08008",
            "city": "Barcelona",
            "region": "Cataluña",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "Motor Valencia",
            "gpsLocation": {"name": "Valencia", "longitude": -0.3763, "latitude": 39.4699},
            "streetNumber": "78",
            "streetAddress": "Avenida del Puerto",
            "postalCode": "46023",
            "city": "Valencia",
            "region": "Valencia",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "AutoSevilla Premium",
            "gpsLocation": {"name": "Sevilla", "longitude": -5.9845, "latitude": 37.3891},
            "streetNumber": "56",
            "streetAddress": "Avenida de la Constitución",
            "postalCode": "41001",
            "city": "Sevilla",
            "region": "Andalucía",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "Bilbao Motor Group",
            "gpsLocation": {"name": "Bilbao", "longitude": -2.9253, "latitude": 43.2627},
            "streetNumber": "34",
            "streetAddress": "Gran Vía de Don Diego López de Haro",
            "postalCode": "48009",
            "city": "Bilbao",
            "region": "País Vasco",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "Málaga Automoción",
            "gpsLocation": {"name": "Málaga", "longitude": -4.4214, "latitude": 36.7213},
            "streetNumber": "92",
            "streetAddress": "Calle Larios",
            "postalCode": "29015",
            "city": "Málaga",
            "region": "Andalucía",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "Zaragoza Cars",
            "gpsLocation": {"name": "Zaragoza", "longitude": -0.8773, "latitude": 41.6488},
            "streetNumber": "15",
            "streetAddress": "Paseo de la Independencia",
            "postalCode": "50001",
            "city": "Zaragoza",
            "region": "Aragón",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "Murcia Premium Motors",
            "gpsLocation": {"name": "Murcia", "longitude": -1.1307, "latitude": 37.9922},
            "streetNumber": "67",
            "streetAddress": "Gran Vía Escultor Salzillo",
            "postalCode": "30004",
            "city": "Murcia",
            "region": "Murcia",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "Alicante Auto",
            "gpsLocation": {"name": "Alicante", "longitude": -0.4907, "latitude": 38.3452},
            "streetNumber": "88",
            "streetAddress": "Avenida de Maisonnave",
            "postalCode": "03003",
            "city": "Alicante",
            "region": "Valencia",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        },
        {
            "name": "Vigo Motor Sport",
            "gpsLocation": {"name": "Vigo", "longitude": -8.7207, "latitude": 42.2406},
            "streetNumber": "21",
            "streetAddress": "Calle Príncipe",
            "postalCode": "36202",
            "city": "Vigo",
            "region": "Galicia",
            "country": "España",
            "countryCode": "ES",
            "type": "business"
        }
    ]

    return random.choice(direcciones)

def generar_fecha_matriculacion():
    dia = random.randint(1, 28)
    mes = random.randint(1, 12)
    anio = random.randint(2018, 2024)
    return f"{dia:02d} / {mes:02d} / {anio}"

def cdata(text):
    return f"<![CDATA[{text}]]>"

def generar_anuncio_xml(indice):
    marca, modelo, version, _ = random.choice(marcas_modelos)
    precio = random.randint(15000, 45000)

    anuncio = ET.Element("anuncio")

    # Datos básicos
    ET.SubElement(anuncio, "motorflashid").text = str(random.randint(10000000, 99999999))
    ET.SubElement(anuncio, "dealerid").text = str(random.randint(10000000, 99999999))
    ET.SubElement(anuncio, "instalacion").text = f"Concesionario {marca}"
    ET.SubElement(anuncio, "mail").text = generar_email()
    ET.SubElement(anuncio, "matricula").text = generar_matricula()
    ET.SubElement(anuncio, "chasis").text = generar_vin()
    ET.SubElement(anuncio, "marca").text = marca
    ET.SubElement(anuncio, "modelo").text = modelo
    ET.SubElement(anuncio, "version").text = version
    ET.SubElement(anuncio, "carroceria").text = generar_body_type()["value"]
    ET.SubElement(anuncio, "estado").text = generar_state_type()["value"]
    ET.SubElement(anuncio, "disponible").text = "DISPONIBLE"

    # Precios
    ET.SubElement(anuncio, "precio").text = str(precio)
    ET.SubElement(anuncio, "precio_nuevo").text = str(int(precio * 1.4))
    ET.SubElement(anuncio, "precio_profesional").text = str(int(precio * 0.9))
    ET.SubElement(anuncio, "precio_ofertafinanciacion").text = str(int(precio * 0.85))

    # Características técnicas
    ET.SubElement(anuncio, "puertas").text = str(random.choice([3, 5]))
    ET.SubElement(anuncio, "cambio").text = generar_change_type()["value"]
    ET.SubElement(anuncio, "ancho").text = str(random.randint(1700, 1900))
    ET.SubElement(anuncio, "largo").text = str(random.randint(4000, 5000))
    ET.SubElement(anuncio, "alto").text = str(random.randint(1400, 1700))
    ET.SubElement(anuncio, "maletero").text = str(random.randint(300, 600))
    ET.SubElement(anuncio, "deposito").text = str(random.randint(45, 75))
    ET.SubElement(anuncio, "aceleracion").text = str(round(random.uniform(6.0, 12.0), 1))
    ET.SubElement(anuncio, "velocidadmax").text = str(random.randint(180, 250))
    ET.SubElement(anuncio, "peso").text = str(random.randint(1200, 2000))
    ET.SubElement(anuncio, "marchas").text = str(random.choice([5, 6, 7, 8]))
    ET.SubElement(anuncio, "kilometros").text = str(random.randint(10000, 200000))
    ET.SubElement(anuncio, "combustible").text = generar_fuel_type()["value"]
    ET.SubElement(anuncio, "potencia").text = str(random.randint(90, 400))
    ET.SubElement(anuncio, "garantia").text = str(random.choice([12, 24, 36]))
    ET.SubElement(anuncio, "iva_deducible").text = str(random.choice([True, False])).lower()
    ET.SubElement(anuncio, "plazas").text = str(random.choice([4, 5, 7]))
    ET.SubElement(anuncio, "traccion").text = generar_drivetraintype_type()["value"]
    ET.SubElement(anuncio, "fechamatriculacion").text = generar_fecha_matriculacion()

    # Fotos
    fotos_elem = ET.SubElement(anuncio, "fotos")
    for foto_url in generar_resources():
        ET.SubElement(fotos_elem, "foto").text = foto_url["resource"]

    # Color
    ET.SubElement(anuncio, "color").text = generar_color_type()["value"]
    ET.SubElement(anuncio, "colortapiceria").text = generar_color_type()["value"]

    # Motor y emisiones
    ET.SubElement(anuncio, "cilindrada").text = str(random.randint(1000, 3000))
    ET.SubElement(anuncio, "emisiones").text = str(random.randint(90, 150))
    ET.SubElement(anuncio, "distintivo").text = generar_environmentalbadge_type()["value"]

    # Consumo
    consumo = ET.SubElement(anuncio, "consumo")
    carretera = round(random.uniform(3.0, 5.5), 1)
    urbano = round(random.uniform(4.0, 7.0), 1)
    mixto = round((carretera + urbano) / 2, 1)
    ET.SubElement(consumo, "carretera").text = str(carretera)
    ET.SubElement(consumo, "urbano").text = str(urbano)
    ET.SubElement(consumo, "mixto").text = str(mixto)

    # Pinturas
    pinturas = ET.SubElement(anuncio, "pinturas")
    pintura1 = ET.SubElement(pinturas, "pintura", cod="A76", precio="0,00")
    pintura1.text = "Pintura metalizada"

    # Tapicerías
    tapicerias = ET.SubElement(anuncio, "tapicerias")
    tapizado1 = ET.SubElement(tapicerias, "tapizado", cod="BDAT", precio="0,00")
    tapizado1.text = "Tela estándar"

    # Extras
    extras_opciones = [
        ("507", "Control de distancia en aparcamiento (PDC) trasero", "513,40"),
        ("544", "Control de crucero con función de frenado", "355,15"),
        ("606", "Sistema de navegación Business", "1.085,68"),
        ("216", "Servotronic", "0,00"),
        ("249", "Volante multifunción", "0,00"),
        ("520", "Faros antiniebla", "0,00"),
        ("521", "Sensor de lluvia", "0,00"),
        ("5A2", "Faros de LED", "0,00"),
        ("6NH", "Dispositivo para manos libres con interfaz USB", "0,00")
    ]

    extras = ET.SubElement(anuncio, "extras")
    for cod, descripcion, precio_extra in random.sample(extras_opciones, random.randint(3, 7)):
        extra = ET.SubElement(extras, "extra", cod=cod, precio=precio_extra)
        extra.text = descripcion

    # Serie (equipamiento de serie)
    serie = ET.SubElement(anuncio, "serie")

    # Exterior
    exterior = ET.SubElement(serie, "exterior")
    equipos_exterior = [
        "Faros con bombilla LED y lente elipsoidal",
        "Llantas de aleación ligera de 17 pulgadas",
        "Retrovisores exteriores eléctricos",
        "Luces antiniebla delanteras",
        "Pintura metalizada"
    ]
    for equipo in random.sample(equipos_exterior, random.randint(2, 4)):
        ET.SubElement(exterior, "equipo").text = equipo

    # Interior
    interior = ET.SubElement(serie, "interior")
    equipos_interior = [
        "Asientos de tela",
        "Volante multifunción revestido de cuero",
        "Climatizador automático bi-zona",
        "Apoyabrazos central delantero",
        "Retrovisor interior antideslumbramiento"
    ]
    for equipo in random.sample(equipos_interior, random.randint(2, 4)):
        ET.SubElement(interior, "equipo").text = equipo

    # Confort
    confort = ET.SubElement(serie, "confort")
    equipos_confort = [
        "Elevalunas eléctricos delanteros y traseros",
        "Aire acondicionado automático",
        "Sistema de navegación",
        "Pantalla táctil de 8 pulgadas",
        "Control de crucero",
        "Sensor de lluvia y luces"
    ]
    for equipo in random.sample(equipos_confort, random.randint(3, 5)):
        ET.SubElement(confort, "equipo").text = equipo

    # Seguridad
    seguridad = ET.SubElement(serie, "seguridad")
    equipos_seguridad = [
        "ABS",
        "Control de estabilidad (ESP)",
        "Airbags frontales, laterales y de cortina",
        "Asistente de arranque en pendiente",
        "Sistema de frenado de emergencia",
        "Sensor de presión de neumáticos",
        "Preparación ISOFIX"
    ]
    for equipo in random.sample(equipos_seguridad, random.randint(4, 7)):
        ET.SubElement(seguridad, "equipo").text = equipo

    # Observaciones
    ET.SubElement(anuncio, "observaciones").text = f"""Vehículo en perfecto estado. 
    
    - Revisión de 120 puntos realizada
    - Certificado de kilometraje
    - Garantía incluida
    - Financiación disponible
    - Aceptamos su vehículo como parte de pago
    
    Precio especial para profesionales. Consulte condiciones.
    """

    ET.SubElement(anuncio, "notainterna").text = f"Ref-{marca.upper()}-{indice:04d}"

    return anuncio


def crear_xml_motorflash(num_anuncios, offers_to_delete=[]):
    """Crea el XML de forma eficiente para grandes volúmenes"""
    motorflash = ET.Element("motorflash")

    # Generar anuncios
    for i in range(num_anuncios):
        anuncio = generar_anuncio_xml(i)
        motorflash.append(anuncio)

    # Agregar ofertas a eliminar
    if offers_to_delete:
        offers_delete_elem = ET.SubElement(motorflash, "offersToDelete")
        for offer_id in offers_to_delete:
            ET.SubElement(offers_delete_elem, "id").text = offer_id

    # Convertir a bytes y luego a string de forma eficiente
    # 'xml_declaration=True' asegura que mantenga la cabecera <?xml...
    return ET.tostring(motorflash, encoding='utf-8', method='xml')

# --- ENTRADA DE DATOS ---
print("--- CONFIGURACIÓN DE ENVÍO ---")

inventory_id = input("Ingresa el inventoryId [856b7d9a-cabd-4e86-aaca-b7a9641a9d0b]: ").strip()
if not inventory_id:
    inventory_id = "856b7d9a-cabd-4e86-aaca-b7a9641a9d0b"
    print(f"Usando inventoryId por defecto: {inventory_id}")

dump_type = input("Ingresa el dumpType (UPDATE/REPLACEMENT) [UPDATE]: ").strip().upper()
if not dump_type:
    dump_type = "UPDATE"

external_pub_id = input("Ingresa el externalPublicationId [localPub1#]: ").strip()
if not external_pub_id:
    external_pub_id = "localPub1#"

offers_to_delete_input = input("Ingresa los offerIds a borrar (separados por comas) o Enter para ninguno: ").strip()
if offers_to_delete_input:
    offers_to_delete = [id.strip() for id in offers_to_delete_input.split(',')]
else:
    offers_to_delete = []

try:
    num_anuncios = int(input("\n¿Cuántos anuncios quieres generar?: "))
except ValueError:
    print("❌ Error: Número no válido.")
    exit()

print(f"\n📦 Generando {num_anuncios} anuncios...")

xml_content = crear_xml_motorflash(num_anuncios, offers_to_delete)

filename = "motorflash_export.xml"
# Nota el 'wb' (write binary) para que no falle con los bytes
with open(filename, 'wb') as f:
    f.write(xml_content)

file_size_bytes = os.path.getsize(filename)
file_size_mb = file_size_bytes / (1024 * 1024)
numero_anuncios_detectados = xml_content.decode('utf-8').count('<anuncio>')

print(f"✅ XML generado correctamente")
print(f"📄 Archivo guardado: {filename}")
print(f"⚖️  Tamaño del archivo: {file_size_bytes} bytes ({file_size_mb:.2f} MB)")
print(f"📊 Total de anuncios: {num_anuncios}")
print(f"📊 Conteo real en archivo: {numero_anuncios_detectados} anuncios")
print(f"—" * 40)

if offers_to_delete:
    print(f"🗑️  Ofertas marcadas para eliminar: {len(offers_to_delete)}")
"""
# --- PREGUNTA PARA EJECUTAR INGESTIÓN ---
ejecutar_ingestion = input("\n🚀 ¿Deseas ejecutar la URL de ingesta ahora? (s/n) [n]: ").strip().lower()

if ejecutar_ingestion == 's':
    # --- TOKEN ---
    url_login = "http://localhost:8888/api/v1/security/login"
    payload_login = json.dumps({
        "client": "cf68a603-9d8f-44bd-9ac3-398d61aa3182",
        "secret": ".Liquidcars1",
        "securityProfile": "M2M_B2C_Channel"
    })
    headers_login = {
        'Content-Type': 'application/json'
    }

    print("\n🔐 Obteniendo token de autenticación...")
    try:
        response_login = requests.post(url_login, headers=headers_login, data=payload_login, timeout=10)
        response_data = response_login.json()
        token_input = response_data.get("access_token")

        if not token_input:
            print("❌ Error: No se pudo obtener el token de acceso")
            exit()

        print("✅ Token obtenido correctamente")
    except Exception as e:
        print(f"❌ Error al obtener token: {e}")
        exit()

    # --- DEFINICIÓN DE URL Y HEADERS ---
    url = f"http://localhost:8890/v1/ingestion/url?format=xml&url=http://localhost:8083/motorflash_export.xml&inventoryId={inventory_id}&dumpType={dump_type}&externalPublicationId={external_pub_id}"

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token_input}"
    }

    print(f"\n🚀 Iniciando envío a {url}...\n")

    exitos = 0
    errores = 0

    try:
        response = requests.post(url, headers=headers, timeout=300)

        if response.ok:
            exitos = 1
            print(f"✅ ÉXITO: XML enviado correctamente")
            print(f"📊 Respuesta del servidor: {response.status_code}")

            try:
                response_json = response.json()
                print(f"\n📄 Detalle de respuesta:")
                print(json.dumps(response_json, indent=2))
            except:
                print(f"\n📄 Respuesta del servidor: {response.text[:500]}")
        else:
            errores = 1
            print(f"❌ Error {response.status_code}: {response.text}")
            if response.status_code == 401:
                print("🛑 Token caducado o inválido")

    except requests.exceptions.Timeout:
        errores = 1
        print(f"⚠️ Timeout: La petición tardó más de 300 segundos")
    except Exception as e:
        errores = 1
        print(f"⚠️ Fallo de conexión: {e}")

    print(f"\n" + "=" * 60)
    print("RESUMEN FINAL")
    print("=" * 60)
    print(f"✅ Éxitos: {exitos}")
    print(f"❌ Errores: {errores}")
    print(f"📄 Archivo XML: {filename}")
else:
    print("\n⏸️  Ingestión omitida. El archivo se mantiene en el directorio para uso manual.")

input("\nPresiona Enter para finalizar...")

# Opcional: Solo borrar el archivo si se ejecutó la ingesta o si tú quieres
borrar = input("🗑️ ¿Deseas eliminar el archivo XML generado? (s/n) [n]: ").strip().lower()
if borrar == 's':
    try:
        os.remove(filename)
        print(f"✅ Archivo {filename} eliminado correctamente")
    except Exception as e:
        print(f"⚠️ Error al eliminar: {e}")
"""