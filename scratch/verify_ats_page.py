import urllib.request
import urllib.parse
import http.cookiejar
import re

cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

# 1. Fetch CSRF token from login page
resp = opener.open('http://localhost:8082/login')
html = resp.read().decode('utf-8')

csrf_match = re.search(r'name="_csrf"\s+value="([^"]+)"', html)
csrf_token = csrf_match.group(1) if csrf_match else ''
print("CSRF Token:", csrf_token)

# 2. Login as student
login_data = urllib.parse.urlencode({
    'username': 'student',
    'password': 'password123',
    '_csrf': csrf_token
}).encode('utf-8')

req = urllib.request.Request('http://localhost:8082/login', data=login_data, headers={'Content-Type': 'application/x-www-form-urlencoded'})
resp = opener.open(req)
print("Post-login URL:", resp.geturl())

# 3. Request ATS Analysis page for Job 3
resp_analysis = opener.open('http://localhost:8082/student/resume/5/analysis/3')
analysis_html = resp_analysis.read().decode('utf-8')

print("\n--- ANALYSIS PAGE STATUS ---")
print("Response code:", resp_analysis.getcode())
if "Something Went Wrong" in analysis_html:
    print("ERROR: Something Went Wrong page returned!")
else:
    print("SUCCESS: ATS Compatibility Report rendered cleanly!")

# Extract ATS Score
ats_match = re.search(r'ATS Score.*?<h2[^>]*>([^<]+)</h2>', analysis_html, re.DOTALL)
if ats_match:
    print("ATS Score:", ats_match.group(1).strip())

# Extract Skill Badges
print("\n--- SKILL BADGES FOUND IN HTML ---")
badge_matches = re.findall(r'<span[^>]*class="[^"]*badge[^"]*"[^>]*style="[^"]*background-color:\s*rgba\(16,\s*185,\s*129[^"]*"[^>]*>([^<]+)</span>', analysis_html)
print(f"Total matched badges: {len(badge_matches)}")
for idx, b in enumerate(badge_matches, 1):
    print(f"  Badge {idx}: [{b.strip()}]")

with open('c:/Users/jaini/.gemini/antigravity-ide/brain/66b8d523-329f-4429-9fc4-cc363dc6361e/scratch/rendered_analysis.html', 'w', encoding='utf-8') as f:
    f.write(analysis_html)
print("Saved rendered HTML to scratch/rendered_analysis.html")
