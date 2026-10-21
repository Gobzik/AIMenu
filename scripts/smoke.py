"""HTTP smoke check for the Docker Compose stack."""
import json
import uuid
import urllib.error
import urllib.request

BASE = 'http://127.0.0.1:8080/api'

def request(path, method='GET', body=None, token=None):
    data = None if body is None else json.dumps(body).encode()
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as response:
        return json.load(response)

email = 'smoke-' + uuid.uuid4().hex + '@example.com'
account = request('/auth/register', 'POST', {
    'email': email, 'password': 'long-password-123',
    'profile': {'allergyProductIds': [], 'favoriteProductIds': [4], 'favoriteRecipeIds': [2]},
})
token = account['token']
assert len(request('/products')) >= 8
assert len(request('/recipes')) >= 4
assert len(request('/recipes/2/shopping-list', token=token)) == 3
request('/pantry/purchases', 'POST', [
    {'productId': 4, 'quantity': 300},
    {'productId': 5, 'quantity': 300, 'expiresAt': '2026-10-25'},
    {'productId': 6, 'quantity': 200, 'expiresAt': '2026-10-25'},
], token)
assert request('/recipes/2/cook', 'POST', {'idempotencyKey': 'smoke-cook'}, token)['replayed'] is False
assert request('/recipes/2/cook', 'POST', {'idempotencyKey': 'smoke-cook'}, token)['replayed'] is True
assert request('/recommendations', token=token)['recommendations']
print('HTTP smoke: PASS')
