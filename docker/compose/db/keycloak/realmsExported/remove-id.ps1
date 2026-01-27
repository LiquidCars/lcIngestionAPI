function Remove-IdProperty {
    param ($obj)

    if ($obj -is [System.Management.Automation.PSCustomObject]) {
        # Es un objeto JSON: eliminar 'id' si existe
        if ($obj.PSObject.Properties.Name -contains 'id') {
            $obj.PSObject.Properties.Remove('id')
        }
        # Recorrer propiedades
        foreach ($prop in $obj.PSObject.Properties) {
            Remove-IdProperty $prop.Value
        }
    }
    elseif ($obj -is [System.Array]) {
        # Es una lista, recorrer todos los elementos
        foreach ($item in $obj) {
            Remove-IdProperty $item
        }
    }
    # Si es otro tipo (string, number), no hace nada
}

$files = Get-ChildItem -Path . -Filter *.json

foreach ($file in $files) {
    Write-Host "Procesando $($file.Name)..."
    $content = Get-Content $file.FullName -Raw
    $json = $content | ConvertFrom-Json
    Remove-IdProperty $json
    $json | ConvertTo-Json -Depth 100 | Set-Content $file.FullName -Encoding UTF8
    Write-Host "Nodos 'id' eliminados en $($file.Name)"
}
