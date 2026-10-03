$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginPage = Invoke-WebRequest -Uri "http://localhost:8082/login" -WebSession $session -UseBasicParsing
$csrf = ($loginPage.InputFields | Where-Object { $_.name -eq '_csrf' }).value

Write-Host "CSRF Token: $csrf"

$body = "username=kashish&password=password123&_csrf=$csrf"
$loginResp = Invoke-WebRequest -Uri "http://localhost:8082/login" -Method Post -Body $body -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing
Write-Host "Post Login Status: $($loginResp.StatusCode) - Redirected to: $($loginResp.BaseResponse.ResponseUri)"

$analysisResp = Invoke-WebRequest -Uri "http://localhost:8082/student/resume/5/analysis/3" -WebSession $session -UseBasicParsing
$html = $analysisResp.Content

[System.IO.File]::WriteAllText("c:\Users\jaini\Documents\AI-ResumeScreeningSystem\scratch\output.html", $html)
Write-Host "Analysis Page Status: $($analysisResp.StatusCode)"

if ($html -match 'Something Went Wrong') {
    Write-Host "CRITICAL BUG: Something Went Wrong page displayed!"
} else {
    Write-Host "SUCCESS: ATS Compatibility Report rendered cleanly without errors!"
}

if ($html -match 'ATS Score.*?<h2[^>]*>([^<]+)</h2>') {
    Write-Host "ATS Score: $($Matches[1].Trim())"
}

Write-Host "`n--- MATCHED SKILLS BADGES IN RENDERED HTML ---"
$regex = [regex]'<span[^>]*class="[^"]*badge[^"]*"[^>]*style="[^"]*background-color:\s*rgba\(16,\s*185,\s*129[^"]*"[^>]*>([^<]+)</span>'
$matches = $regex.Matches($html)
Write-Host "Total Badges Found: $($matches.Count)"
foreach ($m in $matches) {
    Write-Host "  Badge: [$($m.Groups[1].Value.Trim())]"
}
