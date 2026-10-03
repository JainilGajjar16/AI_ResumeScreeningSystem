$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginPage = Invoke-WebRequest -Uri "http://localhost:8082/login" -WebSession $session
$csrf = ($loginPage.InputFields | Where-Object { $_.name -eq '_csrf' }).value

Write-Host "CSRF Token: $csrf"

$body = @{
    username = "student"
    password = "password123"
    _csrf = $csrf
}

$loginResp = Invoke-WebRequest -Uri "http://localhost:8082/login" -Method Post -Body $body -WebSession $session -MaximumRedirection 5
Write-Host "Logged in URL: $($loginResp.BaseResponse.ResponseUri)"

$analysisResp = Invoke-WebRequest -Uri "http://localhost:8082/student/resume/8/analysis/3" -WebSession $session
$html = $analysisResp.Content

if ($html -like "*Something Went Wrong*") {
    Write-Host "ERROR: Something Went Wrong returned!"
} else {
    Write-Host "SUCCESS: ATS Compatibility Report rendered cleanly!"
}

if ($html -match 'ATS Score.*?<h2[^>]*>([^<]+)</h2>') {
    Write-Host "ATS Score: $($Matches[1].Trim())"
}

Write-Host "`n--- MATCHED SKILL BADGES IN HTML ---"
$matches = [regex]::Matches($html, '<span[^>]*class="[^"]*badge[^"]*"[^>]*style="[^"]*background-color:\s*rgba\(16,\s*185,\s*129[^"]*"[^>]*>([^<]+)</span>')
Write-Host "Total Badges Found: $($matches.Count)"
foreach ($m in $matches) {
    Write-Host "Badge: [$($m.Groups[1].Value.Trim())]"
}
