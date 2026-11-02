$ErrorActionPreference = "Stop"

$randomBytes = New-Object byte[] 24
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $generator.GetBytes($randomBytes)
} finally {
    $generator.Dispose()
}

$initialPassword = [Convert]::ToBase64String($randomBytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
$adminEmail = "admin@communitymesh.local"
$env:MESH_ADMIN_EMAIL = $adminEmail
$env:MESH_ADMIN_PASSWORD = $initialPassword
$env:MESH_ADMIN_FIRST_NAME = "Rudra"
$env:MESH_ADMIN_LAST_NAME = "Mishra"

try {
    mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Could not build Community Mesh before provisioning the admin account."
    }

    java -jar target/mesh2-1.0-SNAPSHOT.jar --spring.main.web-application-type=none
    if ($LASTEXITCODE -ne 0) {
        throw "Admin provisioning failed. No initial password is displayed. Check the startup message."
    }
} finally {
    Remove-Item Env:MESH_ADMIN_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:MESH_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:MESH_ADMIN_FIRST_NAME -ErrorAction SilentlyContinue
    Remove-Item Env:MESH_ADMIN_LAST_NAME -ErrorAction SilentlyContinue
}

Write-Host "Admin account: $adminEmail"
Write-Host "Initial password (save this now): $initialPassword"
