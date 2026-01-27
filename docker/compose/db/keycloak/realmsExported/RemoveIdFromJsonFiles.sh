#!/bin/bash

# Directorio a procesar (por defecto el actual)
dir="${1:-$(pwd)}"

echo "Procesando JSON en: $dir"

# Buscar recursivamente archivos .json
find "$dir" -type f -name '*.json' | while IFS= read -r file; do
  echo "Procesando: $file"

  # jq con walk elimina todas las claves "id" de todos los objetos JSON
  if jq 'walk(if type == "object" then del(.id) else . end)' "$file" > "${file}.tmp"; then
    mv "${file}.tmp" "$file"
  else
    echo "❌ Error procesando $file"
    rm -f "${file}.tmp"
  fi
done

echo "✅ Proceso terminado sin archivos .tmp"
