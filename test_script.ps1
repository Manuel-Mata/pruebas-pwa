$base  = "http://localhost:8081/clientes/buscar"
$largo = "A" * 60
$casos = @(
  @("Sin filtros",                      "{}",                                          200),
  @("Pagina 0 de tamano 2",             '{"tamano":2,"pagina":0}',                     200),
  @("Pagina 1 de tamano 2",             '{"tamano":2,"pagina":1}',                     200),
  @("Nombre contiene (ana)",            '{"nombre":"ana"}',                            200),
  @("Nombre y apellido combinados",     '{"nombre":"JUAN","apellidoPaterno":"perez"}', 200),
  @("Nombre vacio se ignora",           '{"nombre":""}',                               200),
  @("Comodin % escapado",               '{"nombre":"%"}',                              200),
  @("Comodin _ escapado",               '{"nombre":"_"}',                              200),
  @("CURP en minusculas",               '{"curp":"pegj900101hdfrrn01"}',               200),
  @("RFC exacto",                       '{"rfc":"PEGJ900101XYZ"}',                     200),
  @("Correo en mayusculas",             '{"correo":"OTRO45@TEST.COM"}',                200),
  @("CURP que no existe",               '{"curp":"ABC"}',                              200),
  @("Activo true",                      '{"activo":true}',                             200),
  @("Activo false",                     '{"activo":false}',                            200),
  @("Activo invalido",                  '{"activo":"quizas"}',                         400),
  @("Numero de cuenta",                 '{"numeroCuenta":"1000000002"}',               200),
  @("Rango de fechas de hoy-ayer",      '{"fechaDesde":"2026-10-08","fechaHasta":"2026-10-08"}', 200),
  @("Fecha futura sin resultados",      '{"fechaDesde":"2030-01-01"}',                 200),
  @("Rango invertido",                  '{"fechaDesde":"2026-12-01","fechaHasta":"2026-01-01"}', 400),
  @("Formato de fecha invalido",        '{"fechaDesde":"31-12-2026"}',                 400),
  @("Orden por fecha desc",             '{"ordenarPor":"fechaRegistro","direccion":"DESC"}', 200),
  @("Orden por campo inventado",        '{"ordenarPor":"inventado"}',                  400),
  @("Tamano excesivo",                  '{"tamano":1000}',                             400),
  @("Tamano cero",                      '{"tamano":0}',                                400),
  @("Pagina negativa",                  '{"pagina":-1}',                               400),
  @("Nombre de 60 caracteres",          ('{"nombre":"' + $largo + '"}'),               400),
  @("CURP de 51 caracteres",            ('{"curp":"' + ("B" * 51) + '"}'),             400),
  @("Combinacion completa",             '{"nombre":"juan","activo":true,"ordenarPor":"fechaRegistro","direccion":"DESC","tamano":5}', 200)
)
$tmp = Join-Path $env:TEMP "resp.json"
$fbody = Join-Path $env:TEMP "req_body.json"

# Obtener token para peticiones autenticadas
$token = $null
try {
    $loginResp = Invoke-RestMethod -Uri "http://localhost:8081/auth/login" -Method Post -Body '{"correo":"otro45@test.com","password":"Password123!"}' -ContentType "application/json"
    $token = if ($loginResp.datos -and $loginResp.datos.token) { $loginResp.datos.token } else { $loginResp.token }
} catch {}

foreach ($c in $casos) {
  [IO.File]::WriteAllText($fbody, $c[1], (New-Object Text.UTF8Encoding($false)))
  $argsList = @("-s", "-o", $tmp, "-w", "%{http_code}", "-X", "POST", $base, "-H", "Content-Type: application/json", "--data-binary", "@$fbody")
  if ($token) { $argsList += @("-H", "Authorization: Bearer $token") }
  $codigo = [int](curl.exe @argsList)
  $cuerpo = "" + (Get-Content $tmp -Raw -Encoding UTF8)
  if ($codigo -eq 200) {
    $j = $cuerpo | ConvertFrom-Json
    $datos = if ($j.datos) { $j.datos } else { $j }
    $extra = "total=$($datos.totalElementos) tamano=$($datos.tamano) paginas=$($datos.totalPaginas)"
  } else {
    $plano = $cuerpo -replace "\s+", " "
    $extra = $plano.Substring(0, [Math]::Min(90, $plano.Length))
  }
  $ok = if ($c[2] -eq 0) { $codigo -lt 500 } else { $codigo -eq $c[2] }
  "{0,-5} {1,-34} esperado={2,-4} obtenido={3,-4} {4}" -f $(if ($ok) {"OK"} else {"FALLA"}), $c[0], $(if ($c[2] -eq 0) {"<500"} else {$c[2]}), $codigo, $extra
}
