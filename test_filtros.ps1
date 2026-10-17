$baseUrl = "http://localhost:8081/clientes/buscar"

# Obtener token primero para pruebas autenticadas
$loginBody = '{"correo":"otro45@test.com","password":"Password123!"}'
$token = $null
try {
    $loginResp = Invoke-RestMethod -Uri "http://localhost:8081/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = if ($loginResp.datos -and $loginResp.datos.token) { $loginResp.datos.token } else { $loginResp.token }
} catch {
    Write-Host "Aviso: No se pudo obtener token previo para test_filtros (servidor apagado o credenciales distintas)"
}

function Test-Endpoint {
    param([string]$name, [string]$bodyJson = "{}")
    Write-Host "--- TEST: $name ---"
    try {
        $headers = @{ "Content-Type" = "application/json" }
        if ($token) { $headers["Authorization"] = "Bearer $token" }
        $response = Invoke-RestMethod -Uri $baseUrl -Method Post -Body $bodyJson -Headers $headers -ErrorAction Stop
        Write-Host "STATUS: 200"
        Write-Host "MENSAJE: $($response.mensaje)"
        if ($response.datos.totalElementos -ne $null) {
            Write-Host "Total Elementos: $($response.datos.totalElementos)"
            if ($response.datos.contenido.Count -gt 0) {
                Write-Host "Primer elemento nombre: $($response.datos.contenido[0].nombreCompleto)"
            }
        } elseif ($response.datos.id -ne $null) {
            Write-Host "Cliente ID: $($response.datos.id) - Nombre: $($response.datos.nombre)"
        }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $body = (New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd()
        Write-Host "STATUS: $status"
        Write-Host "BODY: $body"
    }
    Write-Host "`n"
}

Test-Endpoint -name "POST /clientes/buscar (todos)" -bodyJson '{}'
Test-Endpoint -name "tamano=2, pagina=0" -bodyJson '{"tamano":2,"pagina":0}'
Test-Endpoint -name "nombre=ana" -bodyJson '{"nombre":"ana"}'
Test-Endpoint -name "nombre=JUAN, apellidoPaterno=perez" -bodyJson '{"nombre":"JUAN","apellidoPaterno":"perez"}'
Test-Endpoint -name "curp=peg... (minusculas)" -bodyJson '{"curp":"pegj900101hdfrrn01"}'
Test-Endpoint -name "rfc=PEGJ900101XYZ" -bodyJson '{"rfc":"PEGJ900101XYZ"}'
Test-Endpoint -name "correo=JUAN.CARLOS@TEST.COM" -bodyJson '{"correo":"JUAN.CARLOS@TEST.COM"}'
Test-Endpoint -name "activo=true" -bodyJson '{"activo":true}'
Test-Endpoint -name "activo=false" -bodyJson '{"activo":false}'
Test-Endpoint -name "numeroCuenta=1000000002" -bodyJson '{"numeroCuenta":"1000000002"}'
Test-Endpoint -name "fechaDesde=2026-10-01, fechaHasta=2026-10-31" -bodyJson '{"fechaDesde":"2026-10-01","fechaHasta":"2026-10-31"}'
Test-Endpoint -name "ordenarPor=fechaRegistro, direccion=DESC" -bodyJson '{"ordenarPor":"fechaRegistro","direccion":"DESC"}'
Test-Endpoint -name "buscar por id=2" -bodyJson '{"id":2}'

# Pruebas de validación de entrada
Test-Endpoint -name "ordenarPor inventado (espera 400)" -bodyJson '{"ordenarPor":"inventado"}'
Test-Endpoint -name "tamano=1000 (espera 400)" -bodyJson '{"tamano":1000}'
Test-Endpoint -name "tamano=0 (espera 400)" -bodyJson '{"tamano":0}'
Test-Endpoint -name "pagina=-1 (espera 400)" -bodyJson '{"pagina":-1}'
Test-Endpoint -name "nombre= (60 chars, espera 400)" -bodyJson ('{"nombre":"' + ("a" * 60) + '"}')
