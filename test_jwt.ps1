$base = "http://localhost:8081"
$tmp  = Join-Path $env:TEMP "resp.json"

function Api($metodo, $ruta, $cuerpo = $null, $token = $null) {
  $p = @("-s","-o",$tmp,"-w","%{http_code}","-X",$metodo,"$base$ruta")
  if ($token) { $p += @("-H","Authorization: Bearer $token") }
  if ($cuerpo) {
    $f = Join-Path $env:TEMP "body.json"
    [IO.File]::WriteAllText($f, $cuerpo, (New-Object Text.UTF8Encoding($false)))
    $p += @("-H","Content-Type: application/json","--data-binary","@$f")
  }
  [int](curl.exe @p)
}
function Caso($nombre, $esperado, $obtenido) {
  $t = (Get-Content $tmp -Raw -Encoding UTF8) -replace "\s+"," " -replace '"token":"[^"]+"','"token":"***"'
  if ($t.Length -gt 80) { $t = $t.Substring(0,80) }
  "{0,-5} {1,-46} esperado={2} obtenido={3} {4}" -f $(if ($esperado -eq $obtenido) {"OK"} else {"FALLA"}), $nombre, $esperado, $obtenido, $t
}

$ok = '{"correo":"otro45@test.com","password":"Password123!"}'
Caso "Login correcto" 200 (Api POST "/auth/login" $ok)
$respObj = (Get-Content $tmp -Raw | ConvertFrom-Json)
$token = if ($respObj.datos -and $respObj.datos.token) { $respObj.datos.token } else { $respObj.token }

Caso "Login correo en mayusculas" 200 (Api POST "/auth/login" '{"correo":"OTRO45@TEST.COM","password":"Password123!"}')
Caso "Login contrasena incorrecta" 401 (Api POST "/auth/login" '{"correo":"otro45@test.com","password":"Incorrecta1!"}')
Caso "Login usuario inexistente" 401 (Api POST "/auth/login" '{"correo":"nadie@test.com","password":"Password123!"}')
Caso "Login correo vacio" 400 (Api POST "/auth/login" '{"correo":"","password":"x"}')
Caso "Login cuerpo vacio" 400 (Api POST "/auth/login" '{}')
Caso "Login JSON roto" 400 (Api POST "/auth/login" '{"correo":')
Caso "Login contrasena de 100 caracteres" 400 (Api POST "/auth/login" ('{"correo":"otro45@test.com","password":"' + ("A" * 100) + '"}'))

Caso "POST /clientes/buscar sin token" 401 (Api POST "/clientes/buscar" '{}')
Caso "POST /clientes/buscar con token falso" 401 (Api POST "/clientes/buscar" '{}' "abc.def.ghi")
Caso "POST /clientes/buscar con token alterado" 401 (Api POST "/clientes/buscar" '{}' ($token + "x"))
Caso "POST /clientes/buscar con token" 200 (Api POST "/clientes/buscar" '{}' $token)
Caso "POST /clientes/buscar id:2 con token" 200 (Api POST "/clientes/buscar" '{"id":2}' $token)
Caso "POST /cuentas/buscar sin token" 401 (Api POST "/cuentas/buscar" '{"numeroCuenta":"1000000002"}')
Caso "POST /cuentas/buscar con token" 200 (Api POST "/cuentas/buscar" '{"numeroCuenta":"1000000002"}' $token)

Caso "POST /clientes sin token (publico, cuerpo vacio)" 400 (Api POST "/clientes" '{}')
Caso "POST /catalogos/codigo-postal publico" 200 (Api POST "/catalogos/codigo-postal" '{"cp":"06700"}')
Caso "Swagger publico" 200 (Api GET "/swagger-ui/index.html")
Caso "Actuator health publico" 200 (Api GET "/actuator/health")

Caso "POST /usuarios/buscar sin token" 401 (Api POST "/usuarios/buscar" '{}')
Caso "POST /usuarios/buscar con token" 200 (Api POST "/usuarios/buscar" '{}' $token)
Caso "usuarios activo=true" 200 (Api POST "/usuarios/buscar" '{"activo":true}' $token)
Caso "usuarios activo=quizas" 400 (Api POST "/usuarios/buscar" '{"activo":"quizas"}' $token)
Caso "usuarios sort por passwordHash" 400 (Api POST "/usuarios/buscar" '{"ordenarPor":"passwordHash"}' $token)
Caso "usuarios comodin escapado" 200 (Api POST "/usuarios/buscar" '{"correo":"%"}' $token)

Caso "PUT agregar sin token" 401 (Api PUT "/usuarios/agregar" '{"clienteId":4,"password":"Password123!"}')
Caso "PUT agregar cliente que ya tiene usuario" 409 (Api PUT "/usuarios/agregar" '{"clienteId":4,"password":"Password123!"}' $token)
Caso "PUT agregar cliente inexistente" 404 (Api PUT "/usuarios/agregar" '{"clienteId":99999,"password":"Password123!"}' $token)
Caso "PUT agregar contrasena debil" 400 (Api PUT "/usuarios/agregar" '{"clienteId":4,"password":"12345678"}' $token)
Caso "PUT agregar cuerpo vacio" 400 (Api PUT "/usuarios/agregar" '{}' $token)

Caso "POST /cuentas rechaza saldoInicial (servidor)" 400 (Api POST "/cuentas" '{"clienteId":4,"saldoInicial":500}' $token)

Caso "DELETE /clientes con token" 405 (Api DELETE "/clientes" $null $token)
Caso "Ruta inexistente con token" 404 (Api GET "/ruta-inexistente" $null $token)
Caso "Ruta inexistente sin token" 401 (Api GET "/ruta-inexistente")

Caso "Login cliente 3 activo" 200 (Api POST "/auth/login" '{"correo":"otro@test.com","password":"Password123!"}')
$respOtro = (Get-Content $tmp -Raw | ConvertFrom-Json)
$tokenOtro = if ($respOtro.datos -and $respOtro.datos.token) { $respOtro.datos.token } else { $respOtro.token }

Caso "Baja del cliente 3 (protegido)" 200 (Api PATCH "/clientes/baja" '{"id":3}' $token)
Caso "Login cliente 3 inactivo" 403 (Api POST "/auth/login" '{"correo":"otro@test.com","password":"Password123!"}')
Caso "Token anterior del cliente 3 ya no vale" 401 (Api POST "/clientes/buscar" '{}' $tokenOtro)
