
$root = "D:\Smart-Civic-Issue-System"

# Ask for MySQL password
$secure = Read-Host "Enter MySQL root password" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)

try {
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)

    # Start backend
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$root\backend'; .\mvnw.cmd spring-boot:run"
    )

    # Start frontend
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$root\frontend'; npm run dev"
    )
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
}

Write-Host "Backend and frontend startup commands launched."
