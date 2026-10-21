"""HTTP smoke check for the Docker Compose stack."""
import json
import uuid
import base64
import urllib.error
import http.client
import os
import subprocess
from urllib.parse import urlparse
from datetime import date, timedelta

BASE = os.getenv('AIMENU_BASE', 'http://127.0.0.1:8080/api')

def request(path, method='GET', body=None, token=None):
    data = None if body is None else json.dumps(body).encode()
    headers = {'Content-Type': 'application/json'} if data is not None else {}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if os.name == 'nt':
        command = ['curl.exe', '--silent', '--show-error', '--fail-with-body', '--max-time', '15', '-X', method, BASE + path]
        for name, value in headers.items():
            command += ['-H', f'{name}: {value}']
        if data is not None:
            command += ['--data-binary', '@-']
        result = subprocess.run(command, input=data, capture_output=True)
        if result.returncode:
            raise RuntimeError(f'{method} {path}: {result.stderr.decode()} {result.stdout.decode()}')
        return json.loads(result.stdout)
    parsed = urlparse(BASE)
    connection = http.client.HTTPConnection(parsed.hostname, parsed.port, timeout=15)
    connection.request(method, parsed.path + path, body=data, headers=headers)
    response = connection.getresponse()
    payload = response.read()
    connection.close()
    if response.status >= 400:
        raise RuntimeError(f'{method} {path}: HTTP {response.status}: {payload.decode()}')
    return json.loads(payload)

products = {item['name']: item for item in request('/products')}
recipes = {item['title']: item for item in request('/recipes')}
for item in [*products.values(), *recipes.values()]:
    uuid.UUID(item['id'])
recipe_id = (recipes.get('Курица с рисом') or recipes['Chicken with rice'])['id']
email = 'smoke-' + uuid.uuid4().hex + '@example.com'
account = request('/auth/register', 'POST', {
    'email': email, 'password': 'long-password-123',
    'profile': {'allergyProductIds': [], 'favoriteProductIds': [(products.get('Рис') or products['Rice'])['id']], 'favoriteRecipeIds': [recipe_id]},
})
uuid.UUID(account['userId'])
token = account['token']
header, payload, signature = token.split('.')
assert header and signature
claims = json.loads(base64.urlsafe_b64decode(payload + '=' * (-len(payload) % 4)))
assert uuid.UUID(claims['sub']) == uuid.UUID(account['userId'])
assert claims['exp'] > claims['iat']
try:
    request('/profile', token=token[:-1] + ('A' if token[-1] != 'A' else 'B'))
    raise AssertionError('Tampered JWT was accepted')
except RuntimeError as error:
    assert '401' in str(error), error
assert len(products) >= 8 and len(recipes) >= 4
assert len(request(f'/recipes/{recipe_id}/shopping-list', token=token)) == 3
expires = (date.today() + timedelta(days=7)).isoformat()
request('/pantry/purchases', 'POST', [
    {'productId': (products.get('Рис') or products['Rice'])['id'], 'quantity': 300},
    {'productId': (products.get('Курица') or products['Chicken'])['id'], 'quantity': 300, 'expiresAt': expires},
    {'productId': (products.get('Помидор') or products['Tomato'])['id'], 'quantity': 200, 'expiresAt': expires},
], token)
key = 'smoke-' + uuid.uuid4().hex
assert request(f'/recipes/{recipe_id}/cook', 'POST', {'idempotencyKey': key}, token)['replayed'] is False
assert request(f'/recipes/{recipe_id}/cook', 'POST', {'idempotencyKey': key}, token)['replayed'] is True
assert request('/recommendations', token=token)['recommendations']
print('HTTP smoke: PASS')










# Profile persistence, expiry limit, and recommendation refresh after new stock.
cheese = products.get('Сыр') or products.get('Cheese')
cheese_pasta = recipes.get('Паста с сыром') or recipes.get('Cheese pasta')
assert cheese and cheese_pasta
second = request('/auth/register', 'POST', {
    'email': 'smoke-cheese-' + uuid.uuid4().hex + '@example.com',
    'password': 'long-password-123',
    'profile': {'allergyProductIds': [], 'favoriteProductIds': [], 'favoriteRecipeIds': []},
})
second_token = second['token']
profile = request('/profile', 'PUT', {
    'allergyProductIds': [], 'favoriteProductIds': [cheese['id']], 'favoriteRecipeIds': [],
    'displayName': 'Анна', 'householdName': 'Семья Смирновых', 'adults': 2, 'children': 1,
}, second_token)
assert profile['displayName'] == 'Анна' and profile['adults'] == 2 and profile['children'] == 1
try:
    request('/pantry/purchases', 'POST', [{'productId': cheese['id'], 'quantity': 100, 'expiresAt': '2501-01-01'}], second_token)
    raise AssertionError('Expiry after 2500 was accepted')
except RuntimeError as error:
    assert '400' in str(error), error
request('/pantry/purchases', 'POST', [{'productId': cheese['id'], 'quantity': 100, 'expiresAt': expires}], second_token)
with_cheese = request('/recommendations', token=second_token)['recommendations']
assert cheese_pasta['id'] in [item['recipe_id'] for item in with_cheese]
profile['allergyProductIds'] = [cheese['id']]
request('/profile', 'PUT', profile, second_token)
without_cheese = request('/recommendations', token=second_token)['recommendations']
assert cheese_pasta['id'] not in [item['recipe_id'] for item in without_cheese]
print('Family, expiry, cheese recommendations: PASS')
