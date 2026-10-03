$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# 1. Login
$loginPage = Invoke-WebRequest -Uri "http://localhost:8082/login" -WebSession $session -UseBasicParsing
$csrf = ($loginPage.InputFields | Where-Object { $_.name -eq '_csrf' }).value

$body = "username=student&password=student123&_csrf=$csrf"
$loginResp = Invoke-WebRequest -Uri "http://localhost:8082/login" -Method Post -Body $body -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing
Write-Host "Post Login Status: $($loginResp.StatusCode) - Redirected to: $($loginResp.BaseResponse.ResponseUri)"

# 2. Get CSRF from dashboard or resume page for POST request
$resumePage = Invoke-WebRequest -Uri "http://localhost:8082/student/resume" -WebSession $session -UseBasicParsing
$csrf2 = ($resumePage.InputFields | Where-Object { $_.name -eq '_csrf' }).value[0]
if (-not $csrf2) { $csrf2 = $csrf }
Write-Host "CSRF Token for analyze post: $csrf2"

# 3. Perform POST /student/resume/8/analyze/3
$postBody = "_csrf=$csrf2"
$analyzePost = Invoke-WebRequest -Uri "http://localhost:8082/student/resume/8/analyze/3" -Method Post -Body $postBody -ContentType "application/x-www-form-urlencoded" -WebSession $session -UseBasicParsing
Write-Host "Analyze POST Status Code: $($analyzePost.StatusCode) - Redirected to: $($analyzePost.BaseResponse.ResponseUri)"

# 4. Get Analysis Page HTML
$analysisResp = Invoke-WebRequest -Uri "http://localhost:8082/student/resume/8/analysis/3" -WebSession $session -UseBasicParsing
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
