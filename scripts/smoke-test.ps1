$ErrorActionPreference = "Stop"
$base = "http://localhost:18082"

function Req($method, $url, $token, $body) {
  $headers = @{ "Content-Type" = "application/json" }
  if ($token) { $headers["X-Auth-Token"] = $token }
  if ($null -eq $body) {
    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers
  }
  return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -Body $body
}

Write-Host "1. Dashboard"
$dash = Req GET "$base/api/dashboard" $null $null
Write-Host "activeSos=$($dash.activeSos) incidents=$($dash.activeIncidents)"

Write-Host "2. Login citizen"
$citizen = Req POST "$base/api/accounts/login" $null '{"email":"citizen@mesh.local","password":"password123"}'
Write-Host "citizen role=$($citizen.role)"

Write-Host "3. Login responder"
$responder = Req POST "$base/api/accounts/login" $null '{"email":"responder@mesh.local","password":"password123"}'
Write-Host "responder role=$($responder.role)"

Write-Host "4. Create SOS"
$sos = Req POST "$base/api/sos" $citizen.token '{"category":"Medical","description":"Integration test SOS","latitude":28.614,"longitude":77.21,"locationLabel":"Test Shelter","severity":"HIGH"}'
Write-Host "sos=$($sos.publicCode) status=$($sos.status)"

Write-Host "5. My SOS"
$mine = Req GET "$base/api/sos/mine" $citizen.token $null
Write-Host "mineCount=$($mine.Count)"

Write-Host "6. Profile update"
$profile = Req PUT "$base/api/accounts/me" $citizen.token '{"emergencyContactName":"Test Contact","emergencyContactPhone":"9111111111"}'
Write-Host "emergency=$($profile.emergencyContactName)"

Write-Host "7. Mesh message"
$msg = Req POST "$base/api/mesh/messages" $citizen.token '{"content":"Integration mesh hello"}'
Write-Host "msgId=$($msg.id) delivery=$($msg.deliveryStatus)"

Write-Host "8. Create incident"
$incident = Req POST "$base/api/incidents" $responder.token '{"title":"Test Fire Sector Z","type":"FIRE","sector":"Sector Z","description":"Integration incident","severity":"HIGH"}'
Write-Host "incidentId=$($incident.id)"

Write-Host "9. Assign team"
$teams = Req GET "$base/api/rescue/teams" $null $null
$team = $teams | Where-Object { $_.status -eq "AVAILABLE" } | Select-Object -First 1
if (-not $team) { $team = $teams[0] }
$assignBody = @{ sosAlertId = $sos.id; teamId = $team.id; notes = "Integration assign" } | ConvertTo-Json
$assignment = Req POST "$base/api/rescue/assign" $responder.token $assignBody
Write-Host "assignment=$($assignment.id) status=$($assignment.status)"

Write-Host "10. Advance assignment"
$advanced = Req POST "$base/api/rescue/assignments/$($assignment.id)/advance" $responder.token "{}"
Write-Host "advanced=$($advanced.status)"

Write-Host "11. Register node"
$node = Req POST "$base/api/nodes" $citizen.token '{"displayName":"Test Phone Node","nodeType":"DEVICE","latitude":28.615,"longitude":77.211}'
Write-Host "node=$($node.nodeCode)"

Write-Host "12. Map"
$map = Req GET "$base/api/map" $null $null
Write-Host "map sos=$($map.sosAlerts.Count) teams=$($map.teams.Count) nodes=$($map.nodes.Count)"

Write-Host "13. Static pages"
foreach ($p in @("/", "/dashboard.html", "/sos.html", "/mesh.html", "/network-map.html", "/incidents.html", "/response.html", "/profile.html", "/js/script.js")) {
  $code = (Invoke-WebRequest -Uri ($base + $p) -UseBasicParsing).StatusCode
  if ($code -ne 200) { throw "Page failed $p" }
}
Write-Host "pages OK"

Write-Host "14. Invalid login rejected"
$loginRejected = $false
try {
  Req POST "$base/api/accounts/login" $null '{"email":"citizen@mesh.local","password":"wrong"}' | Out-Null
} catch {
  $statusCode = 0
  if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
    $statusCode = [int]$_.Exception.Response.StatusCode
  }
  if ($statusCode -ne 401) { throw }
  $loginRejected = $true
}
if (-not $loginRejected) { throw "Invalid login unexpectedly succeeded." }
Write-Host "invalid login rejected"

Write-Host "15. Citizen cannot create incident"
$incidentForbidden = $false
try {
  Req POST "$base/api/incidents" $citizen.token '{"title":"Nope","type":"FIRE","sector":"X"}' | Out-Null
} catch {
  $statusCode = 0
  if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
    $statusCode = [int]$_.Exception.Response.StatusCode
  }
  if ($statusCode -ne 403) { throw }
  $incidentForbidden = $true
}
if (-not $incidentForbidden) { throw "Citizen was unexpectedly allowed to create an incident." }
Write-Host "citizen forbidden OK"

Write-Host "ALL INTEGRATION CHECKS PASSED"
